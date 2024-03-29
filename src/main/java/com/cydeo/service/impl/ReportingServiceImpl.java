package com.cydeo.service.impl;

import com.cydeo.dto.CompanyDTO;
import com.cydeo.dto.InvoiceProductDTO;
import com.cydeo.dto.ProductDTO;
import com.cydeo.enums.InvoiceStatus;
import com.cydeo.enums.InvoiceType;
import com.cydeo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {
    private final InvoiceProductService invoiceProductService;
    private final SecurityService securityService;
    private final CompanyService companyService;
    private final ProductService productService;

    public List<InvoiceProductDTO> getInvoiceProductList() {
      return invoiceProductService.findAllApprovedInvoiceInvoiceProduct(InvoiceStatus.APPROVED);
    }

    @Override
    @Transactional
    public List<Map.Entry<String ,BigDecimal>> getMonthlyProfitLossListMap() {

        List<Map.Entry<String ,BigDecimal>> listOfMap = new ArrayList<>();

        // Starting data of the company subscription, get the current user's company inserted day
        long companyId = securityService.getLoggedInUser().getCompany().getId();
        CompanyDTO currentCompany = companyService.findById(companyId);

        LocalDateTime startDate = currentCompany.getInsertDateTime();

        List<LocalDate> keys = mapKeyGenerator(startDate);

        return getMonthlyProfitLossListMapByKeys(keys,companyId);
    }

    @Override
    public List<Map.Entry<String, BigDecimal>> getMonthlyProfitLossByYear(Integer year) {
        long companyId = securityService.getLoggedInUser().getCompany().getId();
        CompanyDTO currentCompany = companyService.findById(companyId);

        LocalDateTime companyStartYear = currentCompany.getInsertDateTime();
        List<LocalDate> keys = mapKeyGenerator(companyStartYear,year);
        return getMonthlyProfitLossListMapByKeys(keys,companyId);
    }

    private List<Map.Entry<String ,BigDecimal>> getMonthlyProfitLossListMapByKeys(List<LocalDate> keys,
                                                                                  Long companyId){
        List<Map.Entry<String ,BigDecimal>> listOfMap = new ArrayList<>();
        for (LocalDate currentKey : keys) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MMMM");
            String key = currentKey.format(formatter);

            BigDecimal profitLoss = invoiceProductService
                    .getProfitLossBasedOneMonth(currentKey.getYear(), currentKey.getMonthValue()
                            , companyId, InvoiceType.SALES);

            listOfMap.add(Map.entry(key.toUpperCase(),profitLoss));
        }
        return listOfMap;
    }

    @Override
    public List<Map.Entry<String, BigDecimal>> getProductProfitLossListMap() {

        List<Map.Entry<String ,BigDecimal>> listOfMap = new ArrayList<>();
        // get the current company id;
        Long companyId = securityService.getLoggedInUser().getCompany().getId();

        List<ProductDTO> allProducts = productService.listAllProducts();

        for (ProductDTO product : allProducts) {
            BigDecimal profitLoss = invoiceProductService
                    .getProductProfitLoss(product.getId(), companyId);
            listOfMap.add(Map.entry(product.getName(),profitLoss));
        }
        return listOfMap;
    }

    private List<LocalDate> mapKeyGenerator(LocalDateTime localDate) {
        int startYear = localDate.getYear();
        Month startMonth = localDate.getMonth();
        int startDay = localDate.getDayOfMonth();

        LocalDate startDate = LocalDate.of(startYear, startMonth, startDay);
        List<LocalDate> mapKeys = new ArrayList<>();
        LocalDate now = LocalDate.now();

        while (startDate.minusMonths(1).isBefore(now)) {
            mapKeys.add(now);
            now = now.minusMonths(1);  // Use minusMonths for descending order
        }
        return mapKeys;
    }

    public static List<LocalDate> mapKeyGenerator(LocalDateTime localDateTime, int year) {
        LocalDate startDate = localDateTime.toLocalDate().minusMonths(1);
        List<LocalDate> mapKeys = new ArrayList<>();
        LocalDate now = LocalDate.now();

        LocalDate endDate;
        if (startDate.getYear() == year && year == now.getYear()) {
            endDate = now;
        } else if (startDate.getYear() == year) {
            endDate = LocalDate.of(year, Month.DECEMBER, 31);
        } else {
            if (year == now.getYear()) {
                endDate = now;
            }else {
                endDate = LocalDate.of(year, Month.DECEMBER, 31);
            }
            startDate = LocalDate.of(year, Month.JANUARY, 1);
        }

        while (startDate.isBefore(endDate)) {
            mapKeys.add(endDate);
            endDate = endDate.minusMonths(1);
        }

        return mapKeys;
    }


    public List<String> generatePageOptions(int dataSize) {
        List<String> pageOptions = new ArrayList<>();
        int pageNum = dataSize / 10;
        int overPageNum = dataSize % 10;
        if (overPageNum != 0) {
            for (int i = 1; i <= pageNum + 1; i++) {
                pageOptions.add("Page " + i);
            }
        } else {
            for (int i = 1; i <= pageNum; i++) {
                pageOptions.add("Page " + i);
            }

        }
        return pageOptions;
    }
    public  <T> List<T> getSublistByPage(List<T> originalList, int pageNumber, int pageSize) {
        int size = originalList.size();

        // Calculate start and end indices based on page number and size
        int startIndex = (pageNumber - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, size);

        // Return the sublist
        return originalList.subList(startIndex, endIndex);
    }

    @Override
    public int getScaleNum() {
        List<Map.Entry<String, BigDecimal>> profitLoss = getProductProfitLossListMap();
        BigDecimal maxProfit = profitLoss.stream()
                .map(Map.Entry::getValue).max(BigDecimal::compareTo).get();
        int scaleNum = maxProfit.divide(BigDecimal.valueOf(100),RoundingMode.HALF_UP).intValue();
        scaleNum += 2;
        return scaleNum;
    }
    public int getScaleNum(List<Map.Entry<String, BigDecimal>> data) {
        BigDecimal maxProfit = data.stream()
                .map(Map.Entry::getValue).max(BigDecimal::compareTo).get();
        int scaleNum = maxProfit.divide(BigDecimal.valueOf(100),RoundingMode.HALF_UP).intValue();
        scaleNum += 2;
        return scaleNum;
    }


    @Override
    public List<String> generatePageOptionsForProfitLoss() {
        List<String> pageOptions = new ArrayList<>();
        int currentCompanyRegisteredYear = securityService.getLoggedInUser().getCompany().getInsertDateTime().getYear();
        while (currentCompanyRegisteredYear<=LocalDate.now().getYear()){
            pageOptions.add(currentCompanyRegisteredYear+"");
            currentCompanyRegisteredYear += 1;
        }
        return pageOptions;
    }
}
