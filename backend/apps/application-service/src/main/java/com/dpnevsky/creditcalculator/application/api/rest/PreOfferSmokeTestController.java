package com.dpnevsky.creditcalculator.application.api.rest;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GeneratePreliminaryOffersRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.PreliminaryOfferResponse;
import com.dpnevsky.creditcalculator.application.api.rest.mapper.PreliminaryOfferResponseMapper;
import com.dpnevsky.creditcalculator.application.application.service.CreatePreliminaryOffersService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PreOfferSmokeTestController {

    private final CreatePreliminaryOffersService createPreliminaryOffersService;
    private final PreliminaryOfferResponseMapper preliminaryOfferResponseMapper;

    public PreOfferSmokeTestController(
            CreatePreliminaryOffersService createPreliminaryOffersService,
            PreliminaryOfferResponseMapper preliminaryOfferResponseMapper
    ) {
        this.createPreliminaryOffersService = createPreliminaryOffersService;
        this.preliminaryOfferResponseMapper = preliminaryOfferResponseMapper;
    }

    @PostMapping("/internal/test/offers/generate")
    public List<PreliminaryOfferResponse> generateOffers(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @Valid @RequestBody GeneratePreliminaryOffersRequest request
    ) {
        if (!"allow".equals(debugAuthHeader)) {
            throw new IllegalStateException("Missing or invalid X-Debug-Auth header");
        }

        return preliminaryOfferResponseMapper.toResponseList(
                createPreliminaryOffersService.create(
                        request.applicationId(),
                        request.requestedAmount(),
                        request.termMonths()
                )
        );
    }
}