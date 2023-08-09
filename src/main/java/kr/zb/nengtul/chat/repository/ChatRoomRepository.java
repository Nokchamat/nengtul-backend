package kr.zb.nengtul.chat.repository;


import java.util.List;
import java.util.Optional;
import kr.zb.nengtul.chat.domain.ChatRoom;
import kr.zb.nengtul.user.domain.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomId(String roomId);

    @EntityGraph(value = "chatRoomWithShareBoardAndConnectedChatRooms")
    List<ChatRoom> findByConnectedChatRoomsUserIdOrderByCreatedAtDesc(User user);
}