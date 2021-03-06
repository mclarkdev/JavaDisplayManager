package org.jdm.api.jenkins;

import java.net.URLEncoder;

import org.jdm.api.APISync;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JenkinsAPI
 * 
 * On creation, a background thread will be started, this thread will, on a user
 * defined sync interval, poll the json endpoint for all stale builds in the
 * cache. A build that is not in the cache will be fetched immediately, this
 * will block on network access. From this point forward, the build will be in
 * the cache, and refreshed on each sync interval - these will not block.
 * 
 * @author Matthew Clark
 */
public final class JenkinsAPI extends APISync {

	private static JenkinsAPI api;

	private JenkinsAPI() {
		super();
	}

	@Override
	public void onUpdate() {

		// iterate through known builds
		for (String key : getKeys()) {

			try {
				// update build
				Build build = updateBuild(key);

				// update cache with a valid build
				if (build != null) {

					set(key, build);
				}
			} catch (Exception e) {

				System.out.println("Failed to update [ " + key + " ]");
				e.printStackTrace();
			}
		}
	}

	private Build updateBuild(String key) throws Exception {

		String[] details = key.split("\\|");

		String host = details[0];
		String name = details[1];

		// job details url
		String encoded = URLEncoder.encode(name, "UTF-8");
		String buildURL = host + "/job/" + encoded + "/lastBuild/api/json";

		// update from json
		try {

			JSONObject json = fetchJSONObject(buildURL);
			Build update = Build.fromJSON(host, name, json);

			if (update != null) {

				// give it the last build
				Build last = (Build) get(key);
				update.setLast(last);

				// update the cache
				set(key, update);
				return update;
			}

		} catch (JSONException e) {

		}

		// default return
		return Build.Invalid(host, name);
	}

	public static JenkinsAPI getInstance() {

		// if no existing instance
		if (api == null) {

			// create one
			synchronized (JenkinsAPI.class) {

				if (api == null) {

					api = new JenkinsAPI();
				}
			}
		}

		// return it
		return api;
	}
}
