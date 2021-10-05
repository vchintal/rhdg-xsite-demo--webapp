package org.everythingjboss.mw.rest;

import java.util.ArrayList;
import java.util.Collections;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;

@ApplicationScoped
@Path("/cache")
public class CacheRestService {

	@Resource(name="rhdg-cache-container")
	private RemoteCacheContainer container;
	private RemoteCache<String, String> cache;
	
	@PostConstruct
	public void init() {
		String cacheName = System.getenv("RHDG_CACHE_NAME");
		this.cache = this.container.getCache((cacheName == null)? "user-xsite-cache":cacheName, true);
	}

	@GET
	@Path("/get")
	@Produces("application/json")
	public CacheOperationResult<CacheEntry<String,String>> get(final @QueryParam("key") String key,
		final @QueryParam("username") String username) {
		final CacheOperationResult<CacheEntry<String,String>> cor = new CacheOperationResult<CacheEntry<String,String>>();
		try {
			ArrayList<CacheEntry<String, String>> cacheEntries = new ArrayList<CacheEntry<String, String>>();
			
			// If a key is provided, get the value for that key in the
		    // cache, else get all entries.
			if (key == null) {
				Set<Map.Entry<String, String>> entries = cache.entrySet();
				for (Map.Entry<String, String> entry : entries) {
					if(entry.getKey().startsWith(username)) {
						String keyWithoutUsername = entry.getKey().replace(username, "");
						cacheEntries.add(new CacheEntry<String, String>(keyWithoutUsername, entry.getValue()));
					}
				}
			} else {
				String value = cache.get(key);
				if (value != null)
					cacheEntries.add(new CacheEntry<String, String>(key, value));
			}

			// Sort all cache entries based on key value.
			Collections.sort(cacheEntries);
			cor.setOutputEntries(cacheEntries);
		} catch(Exception e) {
			cor.setFailed(true);
			cor.setFailureMessage(e.getMessage());
		}
		return cor;
	}

	@PUT
	@Path("/put")
	@Produces("application/json")
	public CacheOperationResult<String> put(final @QueryParam("key") String key,
			final @QueryParam("value") String value,final @QueryParam("username") String username) {
		final CacheOperationResult<String> cor = new CacheOperationResult<String>();
		try {
			String returnValue = cache.putIfAbsent(username.concat(key), value);
			ArrayList<String> returnValues= new ArrayList<String>();
			returnValues.add(returnValue);
			cor.setOutputEntries(returnValues);
		} catch (Exception e) {
			cor.setFailed(true);
			cor.setFailureMessage(e.getMessage());
		}
		return cor;
	}

	@DELETE
	@Path("/remove")
	@Produces("application/json")
	public CacheOperationResult<Boolean> remove(final @QueryParam("key") String key,
			final @QueryParam("value") String value,final @QueryParam("username") String username) {
		final CacheOperationResult<Boolean> cor = new CacheOperationResult<Boolean>();
		try {
			Boolean returnValue = cache.remove(username.concat(key), value);				
			ArrayList<Boolean> returnValues= new ArrayList<Boolean>();
			returnValues.add(returnValue);
			cor.setOutputEntries(returnValues);
		} catch (Exception e) {
			cor.setFailed(true);
			cor.setFailureMessage(e.getMessage());
		}
		return cor;
	}

}
