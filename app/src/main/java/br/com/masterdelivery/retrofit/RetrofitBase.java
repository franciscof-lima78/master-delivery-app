package br.com.masterdelivery.retrofit;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import br.com.masterdelivery.BuildConfig;
import br.com.masterdelivery.activities.LoginActivity;
import br.com.masterdelivery.listeners.RetrofitListener;
import br.com.masterdelivery.models.ErrorObject;
import br.com.masterdelivery.utils.ConfigUtils;
import br.com.masterdelivery.utils.Constants;
import br.com.masterdelivery.utils.HttpUtil;
import br.com.masterdelivery.utils.Logger;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class RetrofitBase {
    public static final String AUTHORIZATION = "Authorization";
    protected Retrofit retrofit;
    protected Context context;
    private Logger logger;


    public RetrofitBase(Context context, boolean addTimeout) {
        this.context = context;


        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient().newBuilder().addInterceptor(interceptor);
        if (addTimeout) {
            httpClientBuilder.readTimeout(Constants.TimeOut.SOCKET_TIME_OUT, TimeUnit.SECONDS);
            httpClientBuilder.connectTimeout(Constants.TimeOut.CONNECTION_TIME_OUT, TimeUnit.SECONDS);
        } else {
            httpClientBuilder.readTimeout(Constants.TimeOut.IMAGE_UPLOAD_SOCKET_TIMEOUT, TimeUnit.SECONDS);
            httpClientBuilder.connectTimeout(Constants.TimeOut.IMAGE_UPLOAD_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        }
        addVersioningHeaders(httpClientBuilder, context);
        OkHttpClient httpClient = httpClientBuilder.build();

        logger = new Logger(RetrofitBase.class.getSimpleName());

        Gson gson = new GsonBuilder()

                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(ConfigUtils.SERVER_URL + ":"
                        + ConfigUtils.SERVER_PORT + "/"
                        + ConfigUtils.APPLICATION_BASE_URL)
                .client(httpClient)

                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

    }

    private void addVersioningHeaders(OkHttpClient.Builder builder, Context context) {
        builder.interceptors().add(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder()
                        .addHeader(AUTHORIZATION, LoginActivity.TOKEN_MASTER_DELIVERY_LOGIN)

                        .build();
                return chain.proceed(request);
            }
        });
    }


    void validateResponse(Response response, RetrofitListener retrofitListener, int apiFlag) {
        if (response.code() == 200) {
            ResponseBody responseBody = (ResponseBody) response.body();
            try {
                retrofitListener.onResponseSuccess(responseBody.string(), apiFlag);
            } catch (IOException e) {
                error(response, retrofitListener, apiFlag);
            }
        } else {
            error(response, retrofitListener, apiFlag);
        }
    }

    private void error(Response response, RetrofitListener retrofitListener, int apiFlag) {
        Gson gson = new Gson();
        ErrorObject errorPojo;
        try {
            errorPojo = gson.fromJson((response.errorBody()).string(), ErrorObject.class);
            if (errorPojo == null) {
                errorPojo = HttpUtil.getServerErrorPojo(context);
            }
            retrofitListener.onResponseError(errorPojo, null, apiFlag);
        } catch (Exception e) {
            retrofitListener.onResponseError(HttpUtil.getServerErrorPojo(context), null, apiFlag);
        }
    }
}
