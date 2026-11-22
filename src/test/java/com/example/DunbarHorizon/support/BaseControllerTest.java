package com.example.DunbarHorizon.support;

import com.example.DunbarHorizon.account.application.service.LoginService;
import com.example.DunbarHorizon.account.application.service.SignupService;
import com.example.DunbarHorizon.account.application.service.UserProfileUpdateService;
import com.example.DunbarHorizon.account.application.service.UserQueryService;
import com.example.DunbarHorizon.account.application.service.VerificationService;
import com.example.DunbarHorizon.account.application.port.out.ProfileImageStoragePort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.notification.application.DeviceTokenService;
import com.example.DunbarHorizon.notification.application.NotificationService;
import com.example.DunbarHorizon.buzz.application.service.BuzzCommentService;
import com.example.DunbarHorizon.buzz.application.service.BuzzQueryService;
import com.example.DunbarHorizon.buzz.application.service.BuzzService;
import com.example.DunbarHorizon.buzz.application.service.BuzzThreadService;
import com.example.DunbarHorizon.flag.application.service.flag.FlagHostService;
import com.example.DunbarHorizon.flag.application.service.flag.FlagModificationService;
import com.example.DunbarHorizon.flag.application.service.flag.FlagParticipationService;
import com.example.DunbarHorizon.flag.application.service.flag.FlagQueryService;
import com.example.DunbarHorizon.flag.application.service.invitation.FlagInvitationCommandService;
import com.example.DunbarHorizon.flag.application.service.invitation.FlagInvitationQueryService;
import com.example.DunbarHorizon.flag.application.service.comment.FlagCommentCommandService;
import com.example.DunbarHorizon.flag.application.service.comment.FlagCommentQueryService;
import com.example.DunbarHorizon.flag.application.service.memorial.FlagMemorialCommandService;
import com.example.DunbarHorizon.flag.application.service.memorial.FlagMemorialQueryService;
import com.example.DunbarHorizon.global.security.JwtAuthenticationFilter;
import com.example.DunbarHorizon.global.security.JwtTokenProvider;
import com.example.DunbarHorizon.global.security.AuthCookieManager;
import com.example.DunbarHorizon.social.application.friendrequest.FriendRequestService;
import com.example.DunbarHorizon.social.application.friendship.FriendshipCommandService;
import com.example.DunbarHorizon.social.application.friendship.FriendshipQueryService;
import com.example.DunbarHorizon.social.application.label.LabelCommandService;
import com.example.DunbarHorizon.social.application.label.LabelQueryService;
import com.example.DunbarHorizon.social.application.network.SocialConnectionPathQueryService;
import com.example.DunbarHorizon.social.application.network.SocialExpansionQueryService;
import com.example.DunbarHorizon.social.application.network.SocialNetworkQueryService;
import com.example.DunbarHorizon.trace.application.TraceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean protected JwtTokenProvider jwtTokenProvider;
    @MockitoBean protected JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean protected AuthCookieManager authCookieManager;

    @MockitoBean protected SignupService signupService;
    @MockitoBean protected LoginService loginService;
    @MockitoBean protected VerificationService verificationService;
    @MockitoBean protected UserProfileUpdateService userProfileUpdateService;
    @MockitoBean protected UserQueryService userQueryService;

    @MockitoBean protected ProfileImageStoragePort profileImageStoragePort;
    @MockitoBean protected ImageStoragePort imageStoragePort;

    @MockitoBean protected BuzzService buzzService;
    @MockitoBean protected BuzzQueryService buzzQueryService;
    @MockitoBean protected BuzzThreadService buzzThreadService;
    @MockitoBean protected BuzzCommentService buzzCommentService;

    @MockitoBean protected FlagHostService flagHostService;
    @MockitoBean protected FlagModificationService flagModificationService;
    @MockitoBean protected FlagParticipationService flagParticipationService;
    @MockitoBean protected FlagQueryService flagQueryService;
    @MockitoBean protected FlagInvitationCommandService flagInvitationCommandService;
    @MockitoBean protected FlagInvitationQueryService flagInvitationQueryService;

    @MockitoBean protected FlagCommentCommandService flagCommentCommandService;
    @MockitoBean protected FlagCommentQueryService flagCommentQueryService;

    @MockitoBean protected FlagMemorialCommandService flagMemorialCommandService;
    @MockitoBean protected FlagMemorialQueryService flagMemorialQueryService;

    @MockitoBean protected FriendRequestService friendRequestService;

    @MockitoBean protected FriendshipCommandService friendshipCommandService;
    @MockitoBean protected FriendshipQueryService friendshipQueryService;

    @MockitoBean protected LabelCommandService labelCommandService;
    @MockitoBean protected LabelQueryService labelQueryService;

    @MockitoBean protected SocialExpansionQueryService socialExpansionQueryService;
    @MockitoBean protected SocialNetworkQueryService socialNetworkQueryService;
    @MockitoBean protected SocialConnectionPathQueryService socialConnectionPathQueryService;

    @MockitoBean protected TraceService traceService;

    @MockitoBean protected NotificationService notificationService;
    @MockitoBean protected DeviceTokenService deviceTokenService;

}
