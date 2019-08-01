package ren.yale.android.retrofitcachetest.rx1;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ren.yale.android.retrofitcachelib.CacheInterceptorListener;
import ren.yale.android.retrofitcachelib.RetrofitCache;
import ren.yale.android.retrofitcachelib.intercept.CacheForceInterceptorNoNet;
import ren.yale.android.retrofitcachelib.intercept.CacheInterceptorOnNet;
import ren.yale.android.retrofitcachelib.transformer.CacheTransformer;
import ren.yale.android.retrofitcachetest.LogTestUtil;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Yale on 2017/6/12.
 */
public enum OKHttpUtilsRx1 {
    INSTANCE;
    private Context mContext;
    private  static ApiRx1 apiRx1;
    public void init(Context context){
        mContext = context;
        if (apiRx1 ==null){
            apiRx1 = configRetrofit(ApiRx1.class,"http://gank.io/api/data/");
        }
        RetrofitCache.getInstance().init(context);
        RetrofitCache.getInstance().setCacheInterceptorListener(
                new CacheInterceptorListener() {
            @Override
            public boolean canCache(Request request,Response response) {
                return true;
            }
        });

    }

    public OkHttpClient getOkHttpClient(){
        okhttp3.OkHttpClient.Builder clientBuilder=new okhttp3.OkHttpClient.Builder();
        clientBuilder.readTimeout(20, TimeUnit.SECONDS);
        clientBuilder.connectTimeout(20, TimeUnit.SECONDS);
        clientBuilder.writeTimeout(20, TimeUnit.SECONDS);
        clientBuilder.addInterceptor(new LogInterceptor());
        //clientBuilder.addInterceptor(new MockInterceptor());
        clientBuilder.addInterceptor(new CacheForceInterceptorNoNet());
        clientBuilder.addNetworkInterceptor(new CacheInterceptorOnNet());
        clientBuilder.retryOnConnectionFailure(true);
        int cacheSize = 200 * 1024 * 1024;
        File cacheDirectory = new File(mContext.getCacheDir(), "httpcache");
        Cache cache = new Cache(cacheDirectory, cacheSize);
        return clientBuilder.cache(cache).build();
    }

    private static void showLog(String str) {
        str = str.trim();
        int index = 0;
        int maxLength = 2000;
        String finalString="";

        while (index < str.length()) {
            if (str.length() <= index + maxLength) {
                finalString = str.substring(index);
            } else {
                finalString = str.substring(index, index+maxLength);
            }
            index += maxLength;
            LogTestUtil.d( finalString.trim());
        }
    }
    private class LogInterceptor implements Interceptor {


        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            StringBuffer sb = new StringBuffer();


            okhttp3.Response response = chain.proceed(chain.request());
            okhttp3.MediaType mediaType = response.body().contentType();
            String content = response.body().string();

            LogTestUtil.d(response.headers().toString());
           // sb.append("======== request: "+request.toString()+"\r\n ======== request headers: "+request.headers().toString()+"\r\n======= response header:"+response.headers().toString()+"\r\n---------- response body:\r\n");
            LogTestUtil.d(sb.toString());
            try {
               // showLog(content);
            }catch (Exception e){
                e.printStackTrace();
            }

            return response.newBuilder()
                    .body(okhttp3.ResponseBody.create(mediaType, content))
                    .build();
        }
    }
    public static <T> Observable.Transformer<T, T> IoMain() {

        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> tObservable) {

                return tObservable.compose(CacheTransformer.<T>emptyTransformer())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).map(new Func1<T, T>() {
                            @Override
                            public Object call(Object t) {

                                return t;
                            }
                        });
            }
        };
    }


    public ApiRx1 getApi(){
        return apiRx1;
    }


    private <T> T configRetrofit(Class<T> service,String url ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        RetrofitCache.getInstance().addRetrofit(retrofit);
        return retrofit.create(service);
    }
}
