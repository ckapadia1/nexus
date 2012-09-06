/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.client.internal.rest.jersey.subsystem;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.ServerConfiguration;
import org.sonatype.nexus.client.core.subsystem.config.HttpProxy;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.AuthenticationSettings;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationResourceResponse;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettings;

public class JerseyServerConfiguration
    extends SubsystemSupport<JerseyNexusClient>
    implements ServerConfiguration
{

    /**
     * Http Proxy sub/subsystem.
     * Lazy initialized on first request.
     */
    private HttpProxy httpProxy;

    public JerseyServerConfiguration( final JerseyNexusClient nexusClient )
    {
        super( nexusClient );
    }

    @Override
    public HttpProxy proxySettings()
    {
        if ( httpProxy == null )
        {
            httpProxy = new JerseyHttpProxy();
        }
        return httpProxy;
    }

    private GlobalConfigurationResource getConfiguration()
    {
        final GlobalConfigurationResourceResponse response = getNexusClient()
            .serviceResource( "global_settings/current" )
            .get( GlobalConfigurationResourceResponse.class );

        return response.getData();
    }

    private void setConfiguration( final GlobalConfigurationResource configuration )
    {
        final GlobalConfigurationResourceResponse request = new GlobalConfigurationResourceResponse();
        request.setData( configuration );

        getNexusClient().serviceResource( "global_settings/current" ).put( request );
    }

    private class JerseyHttpProxy
        implements HttpProxy
    {

        @Override
        public void setTo( final String host, final int port, final String... nonProxyHosts )
        {
            checkNotNull( host, "Host cannot be null" );
            checkArgument( !host.isEmpty(), "Host cannot be empty" );

            final GlobalConfigurationResource configuration = getConfiguration();

            RemoteHttpProxySettings httpProxySettings = configuration.getGlobalHttpProxySettings();
            if ( httpProxySettings == null )
            {
                httpProxySettings = new RemoteHttpProxySettings();
            }
            httpProxySettings.setProxyHostname( host );
            httpProxySettings.setProxyPort( port );
            if ( nonProxyHosts != null && nonProxyHosts.length > 0 )
            {
                httpProxySettings.setNonProxyHosts( Arrays.asList( nonProxyHosts ) );
            }
            else
            {
                httpProxySettings.setNonProxyHosts( null );
            }

            configuration.setGlobalHttpProxySettings( httpProxySettings );

            setConfiguration( configuration );
        }

        @Override
        public void setCredentials( final String username, final String password )
        {
            setCredentials( username, password, null, null );
        }

        @Override
        public void setCredentials( final String username, final String password,
                                    final String ntlmHost, final String ntlmDomain )
        {
            checkNotNull( username, "User name cannot be null" );
            checkArgument( !username.isEmpty(), "User name cannot be empty" );

            final GlobalConfigurationResource configuration = getConfiguration();

            final AuthenticationSettings auth = new AuthenticationSettings();
            auth.setUsername( username );
            auth.setPassword( password );
            auth.setNtlmHost( ntlmHost );
            auth.setNtlmDomain( ntlmDomain );

            configuration.getGlobalHttpProxySettings().setAuthentication( auth );

            setConfiguration( configuration );
        }

        @Override
        public void reset()
        {
            final GlobalConfigurationResource configuration = getConfiguration();
            configuration.setGlobalHttpProxySettings( null );
            setConfiguration( configuration );
        }

    }

}
