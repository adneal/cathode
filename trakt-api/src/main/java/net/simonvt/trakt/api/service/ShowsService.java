package net.simonvt.trakt.api.service;

import java.util.List;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.entity.UpdatedShows;
import retrofit.RetrofitError;
import retrofit.http.GET;
import retrofit.http.Path;

public interface ShowsService {

  @GET("/shows/trending.json/{apikey}") List<TvShow> trending() throws RetrofitError;

  @GET("/shows/updated.json/{apikey}/{since}") UpdatedShows updated(@Path("since") long since)
      throws RetrofitError;
}
