package com.depromeet.schedule.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.depromeet.common.exception.NoSuchDataException;
import com.depromeet.common.utils.CalendarUtils;
import com.depromeet.member.entity.Member;
import com.depromeet.member.repository.MemberRepository;
import com.depromeet.schedule.entity.Attendance;
import com.depromeet.schedule.entity.Schedule;
import com.depromeet.schedule.repository.AttendanceRepository;
import com.depromeet.schedule.repository.ScheduleRepository;

@Service
public class ScheduleService {
	
	@Autowired
	private ScheduleRepository scheduleRepository;
	
	@Autowired
	private AttendanceRepository attendanceRepository;
	
	@Autowired MemberRepository memberRepository;
	
	public List<Schedule> getSchedulesByDate(int year, int month, int week) {
		Date startDate = CalendarUtils.getFirstDayOfWeek(year, month, week);
		Date endDate = CalendarUtils.getFirstDayOfWeek(year, month, week + 1);
		
		return scheduleRepository.findByStartAtBetween(startDate, endDate);
	}
	
	public void setAttendance(Long memberId, Long scheduleId, Long attendanceCode)
			throws AccessDeniedException {
		
		Schedule schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new NoSuchDataException("해당 스터디가 존재하지 않습니다."));
		
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new NoSuchDataException("회원 정보가 유효하지 않습니다."));
		
		if (schedule.getDecidedAttendance()) {
			throw new AccessDeniedException("이미 확정된 출석입니다.");
		}
		
		List<Schedule> schedules =
				this.getScheduleListStartAtSameWeek(schedule);
		
		List<Attendance> attendances = attendanceRepository.findByScheduleScheduleIdIn(
				schedules.stream()
						.map(s -> s.getScheduleId())
						.collect(Collectors.toList()));
		attendances.stream()
				.filter(a -> a.getMember().getMemberId() == memberId)
				.forEach(a -> attendanceRepository.delete(a));
		
		Attendance attendance = new Attendance(member, schedule, attendanceCode);
		
		attendanceRepository.save(attendance);
	}
	
	public List<Schedule> getScheduleListStartAtSameWeek(Schedule schedule) {
		Date startAt = schedule.getStartAt();
		
		int year = CalendarUtils.getYear(startAt);
		int month = CalendarUtils.getMonth(startAt);
		int week = CalendarUtils.getWeek(startAt);
		
		return getSchedulesByDate(year, month, week);
	}
}