package com.edtech.coe.common.config;

import com.edtech.coe.iam.security.JwtAuthenticationFilter;
import com.edtech.coe.iam.security.RestSecurityExceptionHandler;
import com.edtech.coe.iam.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			RestSecurityExceptionHandler exceptionHandler) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(
						ex -> ex.authenticationEntryPoint(exceptionHandler).accessDeniedHandler(exceptionHandler))
				.authorizeHttpRequests(auth -> auth
						// public
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll().requestMatchers("/error")
						.permitAll()

						// user management: platform level only
						.requestMatchers(HttpMethod.POST, "/api/v1/users").hasRole("SUPER_ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "LEADERSHIP")

						// learner lookup: the roles that enroll learners.
						// LearnerLookupServiceImpl limits COE Admin and Practice Head to their own COE.
						.requestMatchers(HttpMethod.GET, "/api/v1/learners")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")

						// COE management: only SUPER_ADMIN changes COEs
						.requestMatchers(HttpMethod.POST, "/api/v1/coes").hasRole("SUPER_ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/coes/**").hasRole("SUPER_ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/coes/**").hasRole("SUPER_ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/coes/**").hasAnyRole("SUPER_ADMIN", "LEADERSHIP")
						.requestMatchers(HttpMethod.GET, "/api/v1/coes").hasAnyRole("SUPER_ADMIN", "LEADERSHIP")

						// lab management: these roles may change labs.
						// LabServiceImpl then limits COE Admin and Practice Head to their own COE.
						.requestMatchers(HttpMethod.POST, "/api/v1/labs")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.PUT, "/api/v1/labs/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/labs/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")

						// lab reading: everyone who works in or oversees a COE, but not learners yet
						.requestMatchers(HttpMethod.GET, "/api/v1/labs/**")
						.hasAnyRole("SUPER_ADMIN", "LEADERSHIP", "COE_ADMIN", "PRACTICE_HEAD", "HR", "MENTOR")
						.requestMatchers(HttpMethod.GET, "/api/v1/labs")
						.hasAnyRole("SUPER_ADMIN", "LEADERSHIP", "COE_ADMIN", "PRACTICE_HEAD", "HR", "MENTOR")

						// batches and enrollments: these roles may change them.
						// BatchServiceImpl and EnrollmentServiceImpl then limit COE Admin
						// and Practice Head to their own COE.
						.requestMatchers(HttpMethod.POST, "/api/v1/batches", "/api/v1/batches/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.PUT, "/api/v1/batches/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/batches/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/batches/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")

						// batch and enrollment reading: not learners yet
						.requestMatchers(HttpMethod.GET, "/api/v1/batches", "/api/v1/batches/**")
						.hasAnyRole("SUPER_ADMIN", "LEADERSHIP", "COE_ADMIN", "PRACTICE_HEAD", "HR", "MENTOR")

						// NEW: reassignment, on paths under /api/v1/sessions/{id}/.
						// These MUST come before the general sessions rules below: the first
						// matching rule wins, and the general GET rule lets mentors in.
						// Listing the free mentors is a Practice Head job, like reassigning.
						.requestMatchers(HttpMethod.GET, "/api/v1/sessions/*/available-mentors")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						// The general POST rule below matches only the plain /api/v1/sessions path.
						// Without this rule, POST .../reassign-mentor would fall through to
						// anyRequest().authenticated(), which lets ANY logged-in user in.
						.requestMatchers(HttpMethod.POST, "/api/v1/sessions/*/reassign-mentor")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						// The history can name health reasons, so mentors and learners are out
						.requestMatchers(HttpMethod.GET, "/api/v1/sessions/*/mentor-changes")
						.hasAnyRole("SUPER_ADMIN", "LEADERSHIP", "COE_ADMIN", "PRACTICE_HEAD", "HR")

						// sessions: these roles may schedule, change, complete and cancel them.
						// SessionServiceImpl then limits COE Admin and Practice Head to their own COE.
						.requestMatchers(HttpMethod.POST, "/api/v1/sessions")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.PUT, "/api/v1/sessions/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/sessions/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/sessions/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")

						// session reading: the whole COE timetable, but not learners yet
						.requestMatchers(HttpMethod.GET, "/api/v1/sessions", "/api/v1/sessions/**")
						.hasAnyRole("SUPER_ADMIN", "LEADERSHIP", "COE_ADMIN", "PRACTICE_HEAD", "HR", "MENTOR")

						// NEW: mentor absences. Only the Practice Head side records and cancels them.
						// The notes can name health reasons, so mentors and learners cannot read them.
						.requestMatchers(HttpMethod.POST, "/api/v1/mentor-unavailability")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/mentor-unavailability/**")
						.hasAnyRole("SUPER_ADMIN", "COE_ADMIN", "PRACTICE_HEAD")
						.requestMatchers(HttpMethod.GET, "/api/v1/mentor-unavailability",
								"/api/v1/mentor-unavailability/**")
						.hasAnyRole("SUPER_ADMIN", "LEADERSHIP", "COE_ADMIN", "PRACTICE_HEAD", "HR")

						// NEW: the mentor change history across a COE (same readers as above)
						.requestMatchers(HttpMethod.GET, "/api/v1/mentor-changes")
						.hasAnyRole("SUPER_ADMIN", "LEADERSHIP", "COE_ADMIN", "PRACTICE_HEAD", "HR")
						// mentor lookup: the roles that book classes and record absences, plus HR and
						// Leadership
						// for the timetable filter. MentorLookupServiceImpl limits everyone but Super
						// Admin and
						// Leadership to their own COE. Mentors and learners have no use for the list.
						.requestMatchers(HttpMethod.GET, "/api/v1/mentors")
						.hasAnyRole("SUPER_ADMIN", "LEADERSHIP", "COE_ADMIN", "PRACTICE_HEAD", "HR")
						// everything else needs a valid token
						.anyRequest().authenticated())
				.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}