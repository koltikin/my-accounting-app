package com.cydeo.repository;

import com.cydeo.entity.ClientVendor;
import com.cydeo.enums.ClientVendorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ClientVendorRepository extends JpaRepository<ClientVendor, Long> {


    Optional<ClientVendor> findById(Long id);
    List<ClientVendor> findAllByCompanyId(Long companyId);
    List<ClientVendor> findByClientVendorType(ClientVendorType clientVendorType);
    boolean existsByClientVendorName (String type);

}