package org.callimachusproject.concepts;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.callimachusproject.auth.DetachedAuthenticationManager;
import org.callimachusproject.auth.RealmManager;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Iri;

@Iri("http://callimachusproject.org/rdf/2009/framework#AuthenticationManager")
public interface AuthenticationManager {

	String getUserIdentifier(Collection<String> tokens) throws OpenRDFException, IOException;

	String getUserLogin(Collection<String> tokens)
			throws OpenRDFException, IOException;

	String getUsernameSetCookie(Collection<String> tokens) throws OpenRDFException, IOException;

	DetachedAuthenticationManager detachAuthenticationManager(String path,
			List<String> domains, RealmManager manager) throws OpenRDFException, IOException;
}
