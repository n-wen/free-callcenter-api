package io.github.nwen.freecallcenterapi.controller;

import io.github.nwen.freecallcenterapi.entity.Extension;
import io.github.nwen.freecallcenterapi.repository.ExtensionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
public class FsXmlController {

    private final ExtensionRepository extensionRepository;

    public FsXmlController(ExtensionRepository extensionRepository) {
        this.extensionRepository = extensionRepository;
    }

    @PostMapping(
            value = "/fs/directory",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public String directory(
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "action", required = false, defaultValue = "") String action
    ) {
        String effectiveDomain = (domain != null && !domain.isEmpty()) ? domain : "default";
        log.info("FreeSWITCH directory request: user={}, domain={}, action={}", user, effectiveDomain, action);

        if (user == null || user.isEmpty()) {
            log.debug("No user specified, returning domain-level config");
            return buildDomainXml(effectiveDomain);
        }
        // 目前分机没有存储domain，所以这里直接使用effectiveDomain即可
        Optional<Extension> extensionOpt = extensionRepository.findByExtensionNumber(user);

        if (extensionOpt.isEmpty()) {
            log.warn("Extension not found: {}", user);
            return buildNotFoundResponse(effectiveDomain, user);
        }

        Extension extension = extensionOpt.get();
        if (!Boolean.TRUE.equals(extension.getEnabled())) {
            log.warn("Extension {} is disabled, not returning dialstring", user);
            return buildNotFoundResponse(effectiveDomain, user);
        }
        log.info("Found extension: {} for domain {}", extension.getExtensionNumber(), effectiveDomain);
        return buildDirectoryXml(effectiveDomain, extension);
    }

    private String buildDirectoryXml(String domain, Extension extension) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<document type=\"freeswitch/xml\">\n");
        xml.append("  <section name=\"directory\">\n");
        xml.append("    <domain name=\"").append(domain).append("\">\n");
        xml.append("      <params>\n");
        xml.append("        <param name=\"dial-string\" value=\"{presence_id=${dialed_user}@${dialed_domain}}${sofia_contact(${dialed_user}@${dialed_domain})}\"/>\n");
        xml.append("      </params>\n");
        xml.append("      <groups>\n");
        xml.append("        <group name=\"default\">\n");
        xml.append("          <users>\n");
        xml.append("            <user id=\"").append(extension.getExtensionNumber()).append("\">\n");
        xml.append("              <params>\n");
        xml.append("                <param name=\"password\" value=\"").append(extension.getPassword()).append("\"/>\n");
        xml.append("              </params>\n");
        xml.append("              <variables>\n");
        xml.append("                <variable name=\"user_context\" value=\"").append(extension.getContext()).append("\"/>\n");
        xml.append("                <variable name=\"effective_caller_id_name\" value=\"").append(extension.getDisplayName()).append("\"/>\n");
        xml.append("                <variable name=\"effective_caller_id_number\" value=\"").append(extension.getExtensionNumber()).append("\"/>\n");
        xml.append("                <variable name=\"domain_name\" value=\"").append(domain).append("\"/>\n");
        xml.append("              </variables>\n");
        xml.append("            </user>\n");
        xml.append("          </users>\n");
        xml.append("        </group>\n");
        xml.append("      </groups>\n");
        xml.append("    </domain>\n");
        xml.append("  </section>\n");
        xml.append("</document>\n");
        return xml.toString();
    }

    private String buildDomainXml(String domain) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<document type=\"freeswitch/xml\">\n");
        xml.append("  <section name=\"directory\">\n");
        xml.append("    <domain name=\"").append(domain).append("\">\n");
        xml.append("      <params>\n");
        xml.append("        <param name=\"dial-string\" value=\"{presence_id=${dialed_user}@${dialed_domain}}${sofia_contact(${dialed_user}@${dialed_domain})}\"/>\n");
        xml.append("      </params>\n");
        xml.append("      <groups>\n");
        xml.append("        <group name=\"default\">\n");
        xml.append("          <users>\n");
        xml.append("          </users>\n");
        xml.append("        </group>\n");
        xml.append("      </groups>\n");
        xml.append("    </domain>\n");
        xml.append("  </section>\n");
        xml.append("</document>\n");
        return xml.toString();
    }

    private String buildNotFoundResponse(String domain, String user) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<document type=\"freeswitch/xml\">\n");
        xml.append("  <section name=\"directory\">\n");
        xml.append("    <domain name=\"").append(domain).append("\">\n");
        xml.append("      <groups>\n");
        xml.append("        <group name=\"default\">\n");
        xml.append("          <users>\n");
        xml.append("          </users>\n");
        xml.append("        </group>\n");
        xml.append("      </groups>\n");
        xml.append("    </domain>\n");
        xml.append("  </section>\n");
        xml.append("</document>\n");
        return xml.toString();
    }
}
