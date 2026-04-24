package io.github.Romariok.orkestro.organization.service;

import io.github.Romariok.orkestro.organization.dto.OrganizationDTO;
import io.github.Romariok.orkestro.organization.mapper.OrganizationMapper;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.OrganizationInvite;
import io.github.Romariok.orkestro.organization.models.OrganizationLink;
import io.github.Romariok.orkestro.organization.dto.OrganizationLinkDTO;
import io.github.Romariok.orkestro.organization.repository.OrganizationInviteRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationLinkRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationInviteService {

    private final OrganizationInviteRepository organizationInviteRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationLinkRepository organizationLinkRepository;
    private final OrganizationUserService organizationUserService;
    private final OrganizationMapper organizationMapper;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_EDIT')")
    public String getInviteCode(Long organizationId) {
        OrganizationInvite invite = organizationInviteRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Invite not found for organization: " + organizationId));
        return invite.getCode();
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_EDIT')")
    public String regenerateInviteCode(Long organizationId) {
        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

        Long currentUserId = securityUtils.getCurrentUserId();
        String code = generateInviteCode();

        OrganizationInvite invite = organizationInviteRepository.findById(organizationId)
                .orElse(OrganizationInvite.builder().organizationId(organizationId).build());
        invite.setCode(code);
        invite.setCreatedByUserId(currentUserId);
        organizationInviteRepository.save(invite);

        return code;
    }

    @Transactional(readOnly = true)
    public OrganizationDTO getOrganizationByInviteCode(String code) {
        OrganizationInvite invite = organizationInviteRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invite code"));
        Organization org = organizationRepository.findById(invite.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
        return buildOrgDto(org);
    }

    @Transactional
    public void joinByInviteCode(String code, String description) {
        OrganizationInvite invite = organizationInviteRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Invalid invite code"));
        organizationUserService.requestToJoinOrganization(invite.getOrganizationId(), description);
    }

    private OrganizationDTO buildOrgDto(Organization org) {
        OrganizationDTO dto = organizationMapper.toDto(org);
        List<OrganizationLink> links = organizationLinkRepository.findByOrganizationId(org.getId());
        dto.setLinks(links.stream()
                .map(l -> new OrganizationLinkDTO(l.getLinkType(), l.getUrl()))
                .toList());
        return dto;
    }

    private String generateInviteCode() {
        final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        final int length = 32;
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
