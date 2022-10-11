package com.muserver.samples.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.muserver.samples.model.Reservation;

@org.springframework.stereotype.Repository
public class Repository {

	List<Reservation> l = new ArrayList<Reservation>();

	public void book(Reservation r1)throws Exception{
		//Validate reservation
		List<Reservation> filtered = l.stream().filter(r->(r.getTime().equals(r1.getTime()) && r.getDate().equals(r1.getDate()))).collect(Collectors.toList());
		if(filtered.size()>0){
			throw new Exception("Same slot is not available");
		}else{
			l.add(r1);
		}
	}
	public List<Reservation> getList(String date)throws Exception{
				
		return l.stream().filter(r->r.getDate().equals(date)).collect(Collectors.toList());
	}
	
}
