package com.harsh.employee.service;

import com.harsh.employee.Enums.SettlementStatus;
import com.harsh.employee.cache.ActiveWorkerCache;
import com.harsh.employee.entity.response.AttendanceLogDto;
import com.harsh.employee.entity.response.PaginatedResponse;
import com.harsh.employee.exception.BusinessException;
import com.harsh.employee.exception.ResourceNotFoundException;
import com.harsh.employee.model.AttendanceLog;
import com.harsh.employee.model.OvertimeEntry;
import com.harsh.employee.model.Site;
import com.harsh.employee.model.Worker;
import com.harsh.employee.repository.AttendanceLogRepository;
import com.harsh.employee.repository.OvertimeEntryRepository;
import com.harsh.employee.repository.SiteRepository;
import com.harsh.employee.repository.WorkerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final AttendanceLogRepository attendanceRepository;
    private final OvertimeEntryRepository overtimeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_ACTIVE_KEY = "attendance:active";
    private static final BigDecimal STANDARD_SHIFT_HOURS = BigDecimal.valueOf(8);
    private static final BigDecimal MAX_SHIFT_LIMIT = BigDecimal.valueOf(16);
    private static final BigDecimal MONTHLY_OT_CAP = BigDecimal.valueOf(60);

    @Transactional
    public void clockIn(Long workerId, Long siteId) {
        String cacheSubKey = String.valueOf(workerId);
        if (redisTemplate.opsForHash().hasKey(REDIS_ACTIVE_KEY, cacheSubKey)) {
            throw new BusinessException("DUPLICATE_CLOCK_IN", "Worker is already clocked in.");
        }

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));
        if (!worker.isActive()) throw new BusinessException("INACTIVE_WORKER", "Worker profile is suspended.");

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site configuration missing."));
        if (!site.isActive()) throw new BusinessException("INACTIVE_SITE", "Target field site is inactive.");

        // Check DB for absolute verification of open logs
        Optional<AttendanceLog> activeLog = attendanceRepository.findByWorkerAndClockOutTimeIsNull(worker);
        if (activeLog.isPresent()) {
            throw new BusinessException("DUPLICATE_CLOCK_IN", "Database transaction contains unclosed session.");
        }

        LocalDateTime now = LocalDateTime.now();
        AttendanceLog log = new AttendanceLog();
        log.setWorker(worker);
        log.setSite(site);
        log.setClockInTime(now);
        attendanceRepository.save(log);

        ActiveWorkerCache cachePayload = new ActiveWorkerCache(workerId, worker.getName(), siteId, site.getSiteName(), now.toString());
        redisTemplate.opsForHash().put(REDIS_ACTIVE_KEY, cacheSubKey, cachePayload);
        // Safety net TTL enforced globally on hash configurations via custom eviction policies or specific keys
    }

    @Transactional
    public void clockOut(Long workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));

        AttendanceLog log = attendanceRepository.findByWorkerAndClockOutTimeIsNull(worker)
                .orElseThrow(() -> new BusinessException("NO_ACTIVE_CLOCK_IN", "Worker is not clocked in anywhere."));

        LocalDateTime clockOutTime = LocalDateTime.now();
        log.setClockOutTime(clockOutTime);

        long minutesDiff = Duration.between(log.getClockInTime(), clockOutTime).toMinutes();
        BigDecimal hoursWorked = BigDecimal.valueOf(minutesDiff).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        log.setTotalHours(hoursWorked);

        if (hoursWorked.compareTo(MAX_SHIFT_LIMIT) > 0) {
            log.setFlagged(true);
        }

        BigDecimal calculatedOvertime = BigDecimal.ZERO;
        if (hoursWorked.compareTo(STANDARD_SHIFT_HOURS) > 0) {
            calculatedOvertime = hoursWorked.subtract(STANDARD_SHIFT_HOURS);
        }

        if (calculatedOvertime.compareTo(BigDecimal.ZERO) > 0) {
            LocalDate logDate = log.getClockInTime().toLocalDate();
            LocalDate startOfMonth = logDate.withDayOfMonth(1);
            LocalDate endOfMonth = logDate.withDayOfMonth(logDate.lengthOfMonth());

            BigDecimal accumulatedOtHours = overtimeRepository.getSumOfOvertimeHoursForMonth(workerId, startOfMonth, endOfMonth);
            if (accumulatedOtHours == null) accumulatedOtHours = BigDecimal.ZERO;

            if (accumulatedOtHours.compareTo(MONTHLY_OT_CAP) < 0) {
                BigDecimal capacityLeft = MONTHLY_OT_CAP.subtract(accumulatedOtHours);
                BigDecimal finalOtToRecord = calculatedOvertime.min(capacityLeft);

                log.setOvertimeHours(finalOtToRecord);

                // Math Strategy: Hourly Breakdown = Daily Wage / 8 Hours
                BigDecimal baseHourlyRate = worker.getDailyWageRate().divide(STANDARD_SHIFT_HOURS, 2, RoundingMode.HALF_UP);
                BigDecimal overtimePayout = BigDecimal.ZERO;

                BigDecimal firstTierHours = finalOtToRecord.min(BigDecimal.valueOf(2));
                BigDecimal secondTierHours = finalOtToRecord.subtract(firstTierHours);

                overtimePayout = overtimePayout.add(firstTierHours.multiply(baseHourlyRate).multiply(BigDecimal.valueOf(1.5)));
                if (secondTierHours.compareTo(BigDecimal.ZERO) > 0) {
                    overtimePayout = overtimePayout.add(secondTierHours.multiply(baseHourlyRate).multiply(BigDecimal.valueOf(2.0)));
                }

                OvertimeEntry otEntry = new OvertimeEntry();
                otEntry.setWorker(worker);
                otEntry.setAttendance(log);
                otEntry.setDate(logDate);
                otEntry.setOvertimeHours(finalOtToRecord);
                otEntry.setOvertimeRateApplied(baseHourlyRate);
                otEntry.setAmount(overtimePayout);
                otEntry.setSettlementStatus(SettlementStatus.PENDING);
                overtimeRepository.save(otEntry);
            } else {
                log.setOvertimeHours(BigDecimal.ZERO);
            }
        } else {
            log.setOvertimeHours(BigDecimal.ZERO);
        }

        attendanceRepository.save(log);
        redisTemplate.opsForHash().delete(REDIS_ACTIVE_KEY, String.valueOf(workerId));
    }

    public List<Object> getActiveWorkers() {
        return redisTemplate.opsForHash().values(REDIS_ACTIVE_KEY);
    }

    public PaginatedResponse<AttendanceLogDto> getAttendanceHistory(
            Long workerId, LocalDateTime from, LocalDateTime to, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("clockInTime").descending());

        Page<AttendanceLog> logPage = attendanceRepository.findByWorkerIdAndClockInTimeBetween(
                workerId, from, to, pageable);

        List<AttendanceLogDto> content = logPage.getContent().stream()
                .map(log -> new AttendanceLogDto(
                        log.getId(),
                        log.getWorker().getName(),
                        log.getSite().getSiteName(),
                        log.getClockInTime(),
                        log.getClockOutTime(),
                        log.getTotalHours(),
                        log.getOvertimeHours(),
                        log.isFlagged()
                )).toList();

        return new PaginatedResponse<>(
                content,
                logPage.getNumber(),
                logPage.getTotalElements(),
                logPage.getTotalPages()
        );
    }
}