package com.cydeo.controller;

import com.cydeo.dto.*;
import com.cydeo.enums.ClientVendorType;
import com.cydeo.enums.InvoiceType;
import com.cydeo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/purchaseInvoices")
public class PurchasesInvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceProductService invoiceProductService;
    private final ClientVendorService clientVendorService;
    private final ProductService productService;

    /**
     * Lists all purchase invoices in the purchase-invoice-list page
     */
    @GetMapping("/list")
    public String listAllPurchaseInvoices(Model model){
        List<InvoiceDTO> invoiceDTOList = invoiceService.findAllInvoices(InvoiceType.PURCHASE);

        model.addAttribute("invoices", invoiceDTOList);

        return "invoice/purchase-invoice-list";
    }

    /**
     * When end-user click "Edit" button they should land on purchase-invoice-update page. This section is the place end-user will add products in that Invoice,
     */
    @GetMapping("/update/{id}")
    public String editInvoice(@PathVariable("id")Long id, Model model){
        InvoiceDTO foundInvoice = invoiceService.findById(id);
        List<InvoiceProductDTO> invoiceProductDTOList = invoiceProductService.findByInvoiceIdAndTotalCalculated(id);
        List<ClientVendorDTO> clientVendorDTOList = clientVendorService.findClientVendorByClientVendorTypeAndCompany(ClientVendorType.VENDOR);

        model.addAttribute("invoice",foundInvoice);
        model.addAttribute("newInvoiceProduct", new InvoiceProductDTO());
        model.addAttribute("products", productService.listAllProducts()); //TODO List All By Company
        model.addAttribute("invoiceProducts", invoiceProductDTOList);
        model.addAttribute("vendors", clientVendorDTOList );

        return "invoice/purchase-invoice-update";
    }

    /**
     * When end-user click "Save" button, invoice should be updated and they should land on purchase-invoice-list page. When click on Add Product button, this product (InvoiceProduct actually) should be saved to database as an InvoiceProduct, and end-user should be redirected to the very same page with updated Product List section below (Invoice Products actually)
     */
    @PostMapping("/update/{id}")
    public String updateInvoice(@PathVariable("id")Long id,  @ModelAttribute("invoice")InvoiceDTO invoiceToUpdate){

        InvoiceDTO foundInvoice = invoiceService.findById(id);

        invoiceService.update(foundInvoice, invoiceToUpdate);

        return "redirect:/purchaseInvoices/update/"+id;
    }

    @PostMapping("/addInvoiceProduct/{id}")
    public String addInvoiceProduct(@Valid @ModelAttribute("newInvoiceProduct")InvoiceProductDTO invoiceProductDTO, BindingResult bindingResult, @PathVariable("id")Long id, Model model){

        if (bindingResult.hasErrors()) {
            InvoiceDTO foundInvoice = invoiceService.findById(id);
            List<InvoiceProductDTO> invoiceProductDTOList = invoiceProductService.findByInvoiceId(id);
            List<ClientVendorDTO> clientVendorDTOList = clientVendorService.findClientVendorByClientVendorTypeAndCompany(ClientVendorType.VENDOR );

            model.addAttribute("invoice",foundInvoice);
            model.addAttribute("products", productService.listAllProducts());
            model.addAttribute("invoiceProducts", invoiceProductDTOList);
            model.addAttribute("vendors", clientVendorDTOList );

            return "invoice/purchase-invoice-update";
        }

        invoiceProductService.create(invoiceProductDTO, id);

        return "redirect:/purchaseInvoices/update/"+id;
    }

    /**
     * When end-user click "Delete" button, invoice should be deleted (soft delete) and they should land on purchase-invoice-list page
     */
    @GetMapping("/delete/{id}")
    public String deleteInvoice(@PathVariable("id")Long invoiceId){

        invoiceService.deleteInvoice(invoiceId);

        return "redirect:/purchaseInvoices/list";
    }

    /**
     * When End-user clicks on the "Approve" button, invoice status should be converted to "Approved" and they should land on purchase-invoice-list page
     */
    @GetMapping("/approve/{id}")
    public String approveInvoice(@PathVariable("id")Long invoiceId){

        invoiceService.approve(invoiceId);

        return "redirect:/purchaseInvoices/list";
    }

    /**
     * When End-User clicks on "Create Purchase Invoice" button, purchase_invoice_create page should be displayed
     */
    @GetMapping("/create")
    public String createInvoice(Model model){
        InvoiceDTO invoice = invoiceService.invoiceGenerator(InvoiceType.PURCHASE);
        List<ClientVendorDTO> clientVendorDTOList = clientVendorService.findClientVendorByClientVendorTypeAndCompany(ClientVendorType.VENDOR);

        model.addAttribute("newPurchaseInvoice", invoice);
        model.addAttribute("vendors", clientVendorDTOList );

        return "invoice/purchase-invoice-create";
    }

    /**
     * When End-user clicks on SAVE button, a new purchase_invoice should be created in the database and end-user should land the purchase_invoice_update page. (because we only created invoice, but there are no products in it... We need to add them in update page)
     */
    @PostMapping("/create")
    public String createInvoice(@Valid @ModelAttribute("newPurchaseInvoice") InvoiceDTO invoice, BindingResult bindingResult, Model model){

        if (bindingResult.hasErrors()){
            List<ClientVendorDTO> clientVendorDTOList = clientVendorService.findClientVendorByClientVendorTypeAndCompany(ClientVendorType.VENDOR);
            model.addAttribute("vendors", clientVendorDTOList );

            return "invoice/purchase-invoice-create";
        }

        InvoiceDTO createdInvoice = invoiceService.create(invoice, InvoiceType.PURCHASE);

        return "redirect:/purchaseInvoices/update/"+createdInvoice.getId();
    }

    /**
     * When end-user clicks on "-" button, related invoice_product should be deleted from current Invoice.
     */
    @GetMapping("/removeInvoiceProduct/{invoiceId}/{invoiceProductId}")
    public String removeInvoiceProductFromInvoice(@PathVariable("invoiceId")Long invoiceId, @PathVariable("invoiceProductId")Long invoiceProductId){
        invoiceProductService.removeInvoiceProductFromInvoice(invoiceId, invoiceProductId);

        return "redirect:/purchaseInvoices/update/"+invoiceId;
    }

    /**
     * As a user, I should be able to print approved Purchases Invoices
     */
    @GetMapping("/print/{invoiceId}")
    public String printPurchaseInvoice(@PathVariable("invoiceId")Long invoiceId , Model model){
        InvoiceDTO invoice = invoiceService.findById(invoiceId);

        List<InvoiceProductDTO> invoiceProductDTOList =  invoiceProductService.findByInvoiceIdAndTotalCalculated(invoiceId);

        model.addAttribute("invoice", invoice);
        model.addAttribute("company", invoice.getCompany());
        model.addAttribute("invoiceProducts", invoiceProductDTOList);

        return "invoice/invoice_print";
    }

}
