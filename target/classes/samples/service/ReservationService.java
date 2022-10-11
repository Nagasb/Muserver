package com.muserver.samples.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.muserver.samples.model.Reservation;

@Service
public class ReservationService {
	
@Autowired
Repository rep;

public void book(Reservation r1)throws Exception{
	rep.book(r1);
}
public List<Reservation> getList(String date)throws Exception{
	return rep.getList(date);
}
}
