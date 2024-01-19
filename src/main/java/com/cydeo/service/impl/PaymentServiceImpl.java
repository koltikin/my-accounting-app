package com.cydeo.service.impl;

import com.cydeo.entity.Company;
import com.cydeo.entity.Payment;
import com.cydeo.enums.Months;
import com.cydeo.repository.CompanyRepository;
import com.cydeo.repository.PaymentRepository;
import com.cydeo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Lazy
public class PaymentServiceImpl implements PaymentService {

    private final CompanyRepository companyRepository;
    private final PaymentRepository paymentRepository;


    public void generateMonthlyPayments(){
        List<Company> companies = companyRepository.findAll();
        companies.removeIf(company -> company.getTitle().equals("CYDEO"));//not generate payment objects for CYDEO

        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();

        for (Company company : companies) {
            for (Months month : Months.values()) {
                Payment payment = new Payment();
                payment.setYear(currentYear);
                payment.setAmount(BigDecimal.valueOf(250));//monthly subscription fee
                payment.setPaymentDate(currentDate.withMonth(month.ordinal() + 1)); // Adding 1 because Months enum starts from 0
                payment.setPaid(false);
//                payment.setCompanyStripeId(company.getStripeId()); // Set Stripe ID if necessary
                payment.setMonth(month);
                payment.setCompany(company);

                paymentRepository.save(payment);
            }
        }
    }

    @Scheduled(cron = "0 0 1 1 * ?") // Run at 1:00 AM on the 1st day of each month
    @EventListener(ContextRefreshedEvent.class) //By using @EventListener(ContextRefreshedEvent.class), we ensure that the generateMonthlyPaymentsScheduled method will only be executed after the Spring application context has been fully initialized
    public void generateMonthlyPaymentsScheduled() {
        generateMonthlyPayments();
    }

}
