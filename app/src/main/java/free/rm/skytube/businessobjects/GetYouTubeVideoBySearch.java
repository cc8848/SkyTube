/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.businessobjects;

import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.gui.app.SkyTubeApp;

/**
 * Get videos corresponding to the user's query (refer to {@link #setQueryString(String)}).
 */
public class GetYouTubeVideoBySearch extends GetYouTubeVideos {

	protected YouTube.Search.List videosList = null;
	private String nextPageToken = null;
	private boolean noMoreVideoPages = false;

	private static final String	TAG = GetYouTubeVideoBySearch.class.getSimpleName();
	protected static final Long	MAX_RESULTS = 45L;


	@Override
	public void init() throws IOException {
		HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
		JsonFactory jsonFactory = com.google.api.client.extensions.android.json.AndroidJsonFactory.getDefaultInstance();
		YouTube youtube = new YouTube.Builder(httpTransport, jsonFactory, null /*timeout here?*/).build();

		videosList = youtube.search().list("id");
		videosList.setFields("items(id/videoId), nextPageToken");
		videosList.setKey(SkyTubeApp.getStr(R.string.API_KEY));
		videosList.setType("video");
		videosList.setMaxResults(MAX_RESULTS);
		nextPageToken = null;
	}


	public void setQueryString(String query) {
		if (videosList != null)
			videosList.setQ(query);
	}


	@Override
	public List<YouTubeVideo> getNextVideos() {
		List<YouTubeVideo> videosList = null;

		if (!noMoreVideoPages()) {
			try {
				// set the page token/id to retrieve
				this.videosList.setPageToken(nextPageToken);

				// communicate with YouTube
				SearchListResponse searchResponse = this.videosList.execute();

				// get videos
				List<SearchResult> searchResultList = searchResponse.getItems();
				if (searchResultList != null) {
					videosList = getVideosList(searchResultList);
				}

				// set the next page token
				nextPageToken = searchResponse.getNextPageToken();

				// if nextPageToken is null, it means that there are no more videos
				if (nextPageToken == null)
					noMoreVideoPages = true;
			} catch (IOException ex) {
				Log.e(TAG, ex.getLocalizedMessage());
			}
		}

		return videosList;
	}


	/**
	 * YouTube's search functionality (i.e. {@link SearchResult} does not return enough information
	 * about the YouTube videos.
	 *
	 * <p>Hence, we need to submit the video IDs to YouTube to retrieve more information about the
	 * given video list.</p>
	 *
	 * @param searchResultList Search results
	 * @return List of {@link YouTubeVideo}s.
	 * @throws IOException
	 */
	private List<YouTubeVideo> getVideosList(List<SearchResult> searchResultList) throws IOException {
		StringBuilder videoIds = new StringBuilder();

		// append the video IDs into a strings (CSV)
		for (SearchResult res : searchResultList) {
			videoIds.append(res.getId().getVideoId());
			videoIds.append(',');
		}

		// get video details by supplying the videos IDs
		GetVideosDetailsByIDs getVideo = new GetVideosDetailsByIDs();
		getVideo.init(videoIds.toString());

		return getVideo.getNextVideos();
	}


	@Override
	public boolean noMoreVideoPages() {
		return noMoreVideoPages;
	}

}
