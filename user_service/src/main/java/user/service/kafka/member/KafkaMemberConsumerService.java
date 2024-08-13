package user.service.kafka.member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import user.service.MemberService;
import user.service.kafka.member.event.RollbackMemberAddToProjectEvent;
import user.service.kafka.project.event.UserAddToProjectEvent;
import user.service.web.dto.member.request.MemberMappingToProjectRequestDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMemberConsumerService {
    private final MemberService memberService;
    private static final String TOPIC = "member-add-to-project-topic";
    private static final String TOPIC1 = "rollback-member-add-to-project-topic";
    
    @KafkaListener(topics = TOPIC, groupId = "project_create_group", containerFactory = "kafkaSendAddMemberToProjectEventListenerContainerFactory")
    public void listenAddMemberToProjectEvent(UserAddToProjectEvent event) {
        try {
            MemberMappingToProjectRequestDto memberMappingToProjectRequestDto = MemberMappingToProjectRequestDto.builder()
                    .projectId(event.getProjectId())
                    .userIds(List.of(event.getUserId()))
                    .isManager(2)
                    .build();
            memberService.memberAddToProject(memberMappingToProjectRequestDto);
            // 처리 로그 출력
            log.info("Processed AddMemberToProjectEvent");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
    @KafkaListener(topics = TOPIC1, groupId = "is-exist-project-by-member-add-to-project-group", containerFactory = "kafkaRollbackMemberAddToProjectEventListenerContainerFactory")
    public void listenRollbackMemberAddToProjectEvent(RollbackMemberAddToProjectEvent event) {
        try {
            memberService.rollbackMemberAddToProject(event);
            // 처리 로그 출력
            log.info("Processed AddMemberToProjectEvent");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
