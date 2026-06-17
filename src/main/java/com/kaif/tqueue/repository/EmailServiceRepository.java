/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.kaif.tqueue.repository;

import com.kaif.tqueue.models.EmailServiceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author kaif
 */
@Repository
public interface EmailServiceRepository extends JpaRepository<EmailServiceModel,Long>{
    
}
