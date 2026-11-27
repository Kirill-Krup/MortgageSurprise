package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;

public interface MicroloanApplicationService {

    MicroloanApplicationResponse apply(MicroloanApplicationRequest request, CustomUserDetails userDetails);

    MicroloanApplicationResponse repay(String applicationId, CustomUserDetails userDetails);
}
