package com.loanportal.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resolves staff from the corporate LDAP directory so the users screen can prefill
 * the right portal account before access is granted.
 */
@ApplicationScoped
public class DirectorySyncService {

    @ConfigProperty(name = "app.directory.provider-url", defaultValue = "ldap://directory.corp.internal:389")
    String providerUrl;

    @ConfigProperty(name = "app.directory.base-dn", defaultValue = "ou=people,dc=corp,dc=internal")
    String baseDn;

    /** Returns the distinguished name of each directory entry matching the supplied account name. */
    public List<String> resolveStaff(String accountName) throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, providerUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        DirContext ctx = new InitialDirContext(env);
        try {
            SearchControls ctl = new SearchControls();
            ctl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String filter = "(&(objectClass=person)(uid=" + accountName + "))";
            List<String> staff = new ArrayList<>();
            NamingEnumeration<SearchResult> found = ctx.search(baseDn, filter, ctl);
            while (found.hasMore()) {
                staff.add(found.next().getNameInNamespace());
            }
            found.close();
            return staff;
        } finally {
            ctx.close();
        }
    }
}
