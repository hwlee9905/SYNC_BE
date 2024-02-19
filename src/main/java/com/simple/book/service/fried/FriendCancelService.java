package com.simple.book.service.fried;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.simple.book.entity.FriendReqEntity;
import com.simple.book.repository.FriendReqRepository;
import com.simple.book.util.DateFmt;

import jakarta.servlet.http.HttpSession;

@Service
public class FriendCancelService {
	
	@Autowired
	private FriendReqRepository friendReqRepository;
	
	@Autowired
	private DateFmt dateFmt;

	public HashMap<String, Object> requestCancle(HttpSession session, String id) {
		HashMap<String, Object> result = new HashMap<>();
		Object myId = session.getAttribute("id");
		if (myId != null) {
			FriendReqEntity entity = friendReqRepository.findByReqIdAndId(id, (String) myId);
			if (entity != null && entity.getAcceptYn().equals("R")) {
				friendReqRepository.saveAndFlush(setEntity(entity, (String) myId));
				result.put("result", true);
			} else {
				result.put("result", "no_request");
			}
		} else {
			result.put("result", "no_session");
		}
		return result;
	}
	
	private FriendReqEntity setEntity(FriendReqEntity entity, String myId) {
		entity.setAcceptYn("C");
		entity.setUpdDate(dateFmt.getDate("yyyyMMdd"));
		entity.setUpdTime(dateFmt.getDate("HHmmss"));
		entity.setUpdId(myId);
		return entity;
	}
}
