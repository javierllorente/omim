package com.mapswithme.maps.widget.placepage;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapswithme.maps.Framework;
import com.mapswithme.maps.MwmActivity;
import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.R;
import com.mapswithme.maps.ads.CompoundNativeAdLoader;
import com.mapswithme.maps.ads.DefaultAdTracker;
import com.mapswithme.maps.ads.Factory;
import com.mapswithme.maps.ads.LocalAdInfo;
import com.mapswithme.maps.api.ParsedMwmRequest;
import com.mapswithme.maps.base.BaseSponsoredAdapter;
import com.mapswithme.maps.bookmarks.data.Bookmark;
import com.mapswithme.maps.bookmarks.data.BookmarkManager;
import com.mapswithme.maps.bookmarks.data.DistanceAndAzimut;
import com.mapswithme.maps.bookmarks.data.FeatureId;
import com.mapswithme.maps.bookmarks.data.MapObject;
import com.mapswithme.maps.bookmarks.data.Metadata;
import com.mapswithme.maps.cian.Cian;
import com.mapswithme.maps.cian.CianAdapter;
import com.mapswithme.maps.cian.RentPlace;
import com.mapswithme.maps.downloader.CountryItem;
import com.mapswithme.maps.downloader.DownloaderStatusIcon;
import com.mapswithme.maps.downloader.MapManager;
import com.mapswithme.maps.editor.Editor;
import com.mapswithme.maps.editor.OpeningHours;
import com.mapswithme.maps.editor.data.TimeFormatUtils;
import com.mapswithme.maps.editor.data.Timetable;
import com.mapswithme.maps.gallery.FullScreenGalleryActivity;
import com.mapswithme.maps.gallery.GalleryActivity;
import com.mapswithme.maps.gallery.Image;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.maps.review.Review;
import com.mapswithme.maps.routing.RoutingController;
import com.mapswithme.maps.taxi.TaxiManager;
import com.mapswithme.maps.ugc.Impress;
import com.mapswithme.maps.ugc.UGCController;
import com.mapswithme.maps.viator.Viator;
import com.mapswithme.maps.viator.ViatorAdapter;
import com.mapswithme.maps.viator.ViatorProduct;
import com.mapswithme.maps.widget.ArrowView;
import com.mapswithme.maps.widget.BaseShadowController;
import com.mapswithme.maps.widget.LineCountTextView;
import com.mapswithme.maps.widget.ObservableScrollView;
import com.mapswithme.maps.widget.RatingView;
import com.mapswithme.maps.widget.ScrollViewShadowController;
import com.mapswithme.maps.widget.recycler.ItemDecoratorFactory;
import com.mapswithme.maps.widget.recycler.RecyclerClickListener;
import com.mapswithme.maps.widget.recycler.SingleChangeItemAnimator;
import com.mapswithme.util.ConnectionState;
import com.mapswithme.util.Graphics;
import com.mapswithme.util.NetworkPolicy;
import com.mapswithme.util.StringUtils;
import com.mapswithme.util.ThemeUtils;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.Utils;
import com.mapswithme.util.concurrency.UiThread;
import com.mapswithme.util.log.Logger;
import com.mapswithme.util.log.LoggerFactory;
import com.mapswithme.util.sharing.ShareOption;
import com.mapswithme.util.statistics.AlohaHelper;
import com.mapswithme.util.statistics.Statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.mapswithme.util.statistics.Statistics.EventName.PP_HOTEL_DESCRIPTION_LAND;
import static com.mapswithme.util.statistics.Statistics.EventName.PP_HOTEL_FACILITIES;
import static com.mapswithme.util.statistics.Statistics.EventName.PP_HOTEL_GALLERY_OPEN;
import static com.mapswithme.util.statistics.Statistics.EventName.PP_HOTEL_REVIEWS_LAND;
import static com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_DETAILS;
import static com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_OPENTABLE;
import static com.mapswithme.util.statistics.Statistics.EventName.PP_SPONSORED_ACTION;

public class PlacePageView extends RelativeLayout
    implements View.OnClickListener,
               View.OnLongClickListener,
               Sponsored.OnPriceReceivedListener,
               Sponsored.OnHotelInfoReceivedListener,
               LineCountTextView.OnLineCountCalculatedListener,
               RecyclerClickListener,
               NearbyAdapter.OnItemClickListener,
               BottomPlacePageAnimationController.OnBannerOpenListener,
               EditBookmarkFragment.EditBookmarkListener,
               BannerController.BannerListener,
               Viator.ViatorListener,
               BaseSponsoredAdapter.ItemSelectedListener,
               Cian.CianListener
{
  private static final Logger LOGGER = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC);
  private static final String TAG = PlacePageView.class.getSimpleName();
  private static final String PREF_USE_DMS = "use_dms";

  private boolean mIsDocked;
  private boolean mIsFloating;

  // Preview.
  private ViewGroup mPreview;
  private Toolbar mToolbar;
  private TextView mTvTitle;
  private TextView mTvSecondaryTitle;
  private TextView mTvSubtitle;
  private ArrowView mAvDirection;
  private TextView mTvDistance;
  private TextView mTvAddress;
  private View mPreviewRatingInfo;
  private RatingView mRatingView;
  private TextView mTvSponsoredPrice;
  // Details.
  private NestedScrollView mDetails;
  private View mPhone;
  private TextView mTvPhone;
  private View mWebsite;
  private TextView mTvWebsite;
  private TextView mTvLatlon;
  private View mOpeningHours;
  private TextView mFullOpeningHours;
  private TextView mTodayOpeningHours;
  private View mWifi;
  private View mEmail;
  private TextView mTvEmail;
  private View mOperator;
  private TextView mTvOperator;
  private View mCuisine;
  private TextView mTvCuisine;
  private View mWiki;
  private View mEntrance;
  private TextView mTvEntrance;
  private View mTaxiShadow;
  private View mTaxiDivider;
  private View mTaxi;
  private View mEditPlace;
  private View mAddOrganisation;
  private View mAddPlace;
  private View mLocalAd;
  private TextView mTvLocalAd;
  private View mEditTopSpace;
  // Bookmark
  private View mBookmarkFrame;
  private WebView mWvBookmarkNote;
  private TextView mTvBookmarkNote;
  private boolean mBookmarkSet;
  // Place page buttons
  private PlacePageButtons mButtons;
  private ImageView mBookmarkButtonIcon;
  // Hotel
  private View mHotelDescription;
  private LineCountTextView mTvHotelDescription;
  private View mHotelMoreDescription;
  private View mHotelFacilities;
  private View mHotelMoreFacilities;
  private View mHotelGallery;
  private RecyclerView mRvHotelGallery;
  private View mHotelNearby;
  private View mHotelReview;
  private TextView mHotelRating;
  private TextView mHotelRatingBase;
  private View mHotelMore;
  private View mSponsoredGalleryView;
  private RecyclerView mRvSponsoredProducts;
  @Nullable
  private BaseSponsoredAdapter mSponsoredAdapter;
  private TextView mTvSponsoredTitle;
  private ImageView mIvSponsoredLogo;

  @Nullable
  UGCController mUgcController;

  @Nullable
  BannerController mBannerController;

  @Nullable
  View mHeightCompensationView;

  // Animations
  private BaseShadowController mShadowController;
  private BasePlacePageAnimationController mAnimationController;
  private MwmActivity.LeftAnimationTrackListener mLeftAnimationTrackListener;
  // Data
  @Nullable
  private MapObject mMapObject;
  @Nullable
  private Sponsored mSponsored;
  private String mSponsoredPrice;
  private boolean mIsLatLonDms;
  @NonNull
  private final FacilitiesAdapter mFacilitiesAdapter = new FacilitiesAdapter();
  @NonNull
  private final GalleryAdapter mGalleryAdapter;
  @NonNull
  private final NearbyAdapter mNearbyAdapter = new NearbyAdapter(this);
  @NonNull
  private final ReviewAdapter mReviewAdapter = new ReviewAdapter();

  // Downloader`s stuff
  private DownloaderStatusIcon mDownloaderIcon;
  private TextView mDownloaderInfo;
  private int mStorageCallbackSlot;
  private CountryItem mCurrentCountry;

  private final int mMarginBase;

  private final MapManager.StorageCallback mStorageCallback = new MapManager.StorageCallback()
  {
    @Override
    public void onStatusChanged(List<MapManager.StorageCallbackData> data)
    {
      if (mCurrentCountry == null)
        return;

      for (MapManager.StorageCallbackData item : data)
        if (mCurrentCountry.id.equals(item.countryId))
        {
          updateDownloader();
          return;
        }
    }

    @Override
    public void onProgress(String countryId, long localSize, long remoteSize)
    {
      if (mCurrentCountry != null && mCurrentCountry.id.equals(countryId))
        updateDownloader();
    }
  };

  private final Runnable mDownloaderDeferredDetachProc = new Runnable()
  {
    @Override
    public void run()
    {
      detachCountry();
    }
  };

  public enum State
  {
    HIDDEN,
    PREVIEW,
    DETAILS,
    FULLSCREEN
  }

  public interface SetMapObjectListener
  {
    void onSetMapObjectComplete();
  }

  public PlacePageView(Context context)
  {
    this(context, null, 0);
  }

  public PlacePageView(Context context, AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  public PlacePageView(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs);

    mIsLatLonDms = MwmApplication.prefs().getBoolean(PREF_USE_DMS, false);
    mGalleryAdapter = new GalleryAdapter(context);
    mMarginBase = (int) getResources().getDimension(R.dimen.margin_base);
    init(attrs, defStyleAttr);
  }

  public ViewGroup GetPreview() { return mPreview; }

  public boolean isHorizontalScrollAreaTouched(@NonNull MotionEvent event)
  {
    return UiUtils.isViewTouched(event, mHotelGallery)
           || UiUtils.isViewTouched(event, mRvSponsoredProducts);
  }

  public void onActivityResume()
  {
    if (mBannerController != null)
      mBannerController.onChangedVisibility(true);
  }

  public void onActivityPause()
  {
    if (mBannerController != null)
      mBannerController.onChangedVisibility(false);
  }

  public void onActivityStopped()
  {
    if (mBannerController != null)
      mBannerController.detach();
  }

  public void onActivityStarted()
  {
    if (mBannerController != null)
      mBannerController.attach();
  }

  private void initViews()
  {
    LayoutInflater.from(getContext()).inflate(R.layout.place_page, this);

    mPreview = (ViewGroup) findViewById(R.id.pp__preview);
    mTvTitle = (TextView) mPreview.findViewById(R.id.tv__title);
    mTvSecondaryTitle = (TextView) mPreview.findViewById(R.id.tv__secondary_title);
    mToolbar = (Toolbar) findViewById(R.id.toolbar);
    mTvSubtitle = (TextView) mPreview.findViewById(R.id.tv__subtitle);

    View directionFrame = mPreview.findViewById(R.id.direction_frame);
    mTvDistance = (TextView) mPreview.findViewById(R.id.tv__straight_distance);
    mAvDirection = (ArrowView) mPreview.findViewById(R.id.av__direction);
    directionFrame.setOnClickListener(this);

    mTvAddress = (TextView) mPreview.findViewById(R.id.tv__address);

    mPreviewRatingInfo = mPreview.findViewById(R.id.preview_rating_info);
    mRatingView = (RatingView) mPreviewRatingInfo.findViewById(R.id.rating_view);
    mTvSponsoredPrice = (TextView) mPreviewRatingInfo.findViewById(R.id.tv__hotel_price);

    mDetails = (NestedScrollView) findViewById(R.id.pp__details);
    RelativeLayout address = (RelativeLayout) mDetails.findViewById(R.id.ll__place_name);
    mPhone = mDetails.findViewById(R.id.ll__place_phone);
    mPhone.setOnClickListener(this);
    mTvPhone = (TextView) mDetails.findViewById(R.id.tv__place_phone);
    mWebsite = mDetails.findViewById(R.id.ll__place_website);
    mWebsite.setOnClickListener(this);
    mTvWebsite = (TextView) mDetails.findViewById(R.id.tv__place_website);
    LinearLayout latlon = (LinearLayout) mDetails.findViewById(R.id.ll__place_latlon);
    latlon.setOnClickListener(this);
    mTvLatlon = (TextView) mDetails.findViewById(R.id.tv__place_latlon);
    mOpeningHours = mDetails.findViewById(R.id.ll__place_schedule);
    mFullOpeningHours = (TextView) mDetails.findViewById(R.id.opening_hours);
    mTodayOpeningHours = (TextView) mDetails.findViewById(R.id.today_opening_hours);
    mWifi = mDetails.findViewById(R.id.ll__place_wifi);
    mEmail = mDetails.findViewById(R.id.ll__place_email);
    mEmail.setOnClickListener(this);
    mTvEmail = (TextView) mEmail.findViewById(R.id.tv__place_email);
    mOperator = mDetails.findViewById(R.id.ll__place_operator);
    mOperator.setOnClickListener(this);
    mTvOperator = (TextView) mOperator.findViewById(R.id.tv__place_operator);
    mCuisine = mDetails.findViewById(R.id.ll__place_cuisine);
    mTvCuisine = (TextView) mCuisine.findViewById(R.id.tv__place_cuisine);
    mWiki = mDetails.findViewById(R.id.ll__place_wiki);
    mWiki.setOnClickListener(this);
    mEntrance = mDetails.findViewById(R.id.ll__place_entrance);
    mTvEntrance = (TextView) mEntrance.findViewById(R.id.tv__place_entrance);
    mTaxiShadow = mDetails.findViewById(R.id.place_page_taxi_shadow);
    mTaxiDivider = mDetails.findViewById(R.id.place_page_taxi_divider);
    mTaxi = mDetails.findViewById(R.id.ll__place_page_taxi);
    TextView orderTaxi = (TextView) mTaxi.findViewById(R.id.tv__place_page_order_taxi);
    orderTaxi.setOnClickListener(this);
    mEditPlace = mDetails.findViewById(R.id.ll__place_editor);
    mEditPlace.setOnClickListener(this);
    mAddOrganisation = mDetails.findViewById(R.id.ll__add_organisation);
    mAddOrganisation.setOnClickListener(this);
    mAddPlace = mDetails.findViewById(R.id.ll__place_add);
    mAddPlace.setOnClickListener(this);
    mLocalAd = mDetails.findViewById(R.id.ll__local_ad);
    mLocalAd.setOnClickListener(this);
    mTvLocalAd = (TextView) mLocalAd.findViewById(R.id.tv__local_ad);
    mEditTopSpace = mDetails.findViewById(R.id.edit_top_space);
    latlon.setOnLongClickListener(this);
    address.setOnLongClickListener(this);
    mPhone.setOnLongClickListener(this);
    mWebsite.setOnLongClickListener(this);
    mOpeningHours.setOnLongClickListener(this);
    mEmail.setOnLongClickListener(this);
    mOperator.setOnLongClickListener(this);
    mWiki.setOnLongClickListener(this);

    mBookmarkFrame = mDetails.findViewById(R.id.bookmark_frame);
    mWvBookmarkNote = (WebView) mBookmarkFrame.findViewById(R.id.wv__bookmark_notes);
    mWvBookmarkNote.getSettings().setJavaScriptEnabled(false);
    mTvBookmarkNote = (TextView) mBookmarkFrame.findViewById(R.id.tv__bookmark_notes);
    mBookmarkFrame.findViewById(R.id.tv__bookmark_edit).setOnClickListener(this);

    ViewGroup ppButtons = (ViewGroup) findViewById(R.id.pp__buttons).findViewById(R.id.container);

    mHeightCompensationView = findViewById(R.id.pp__height_compensation);

    mHotelMore = findViewById(R.id.ll__more);
    mHotelMore.setOnClickListener(this);

    initHotelDescriptionView();
    initHotelFacilitiesView();
    initHotelGalleryView();
    initHotelNearbyView();
    initHotelRatingView();

    initSponsoredGalleryView();

    mUgcController = new UGCController(this);

    View bannerView = findViewById(R.id.banner);
    if (bannerView != null)
    {
      DefaultAdTracker tracker = new DefaultAdTracker();
      CompoundNativeAdLoader loader = Factory.createCompoundLoader(tracker, tracker);
      mBannerController = new BannerController(bannerView, this, loader, tracker);
    }

    mButtons = new PlacePageButtons(this, ppButtons, new PlacePageButtons.ItemListener()
    {
      @Override
      public void onPrepareVisibleView(PlacePageButtons.Item item, View frame, ImageView icon, TextView title)
      {
        int color;

        switch (item)
        {
          case BOOKING:
            frame.setBackgroundResource(R.drawable.button_booking);
            color = Color.WHITE;
            break;

          case BOOKING_SEARCH:
            frame.setBackgroundResource(R.drawable.button_booking);
            color = Color.WHITE;
            break;

          case OPENTABLE:
            frame.setBackgroundResource(R.drawable.button_opentable);
            color = Color.WHITE;
            break;

          case THOR:
            frame.setBackgroundResource(R.drawable.button_thor);
            color = Color.WHITE;
            break;

          case BOOKMARK:
            mBookmarkButtonIcon = icon;
            updateBookmarkButton();
            color = ThemeUtils.getColor(getContext(), R.attr.iconTint);
            break;

          default:
            color = ThemeUtils.getColor(getContext(), R.attr.iconTint);
            icon.setColorFilter(color);
            break;
        }

        title.setTextColor(color);
      }

      @Override
      public void onItemClick(PlacePageButtons.Item item)
      {
        switch (item)
        {
        case BOOKMARK:
          if (mMapObject == null)
          {
            LOGGER.e(TAG, "Bookmark cannot be managed, mMapObject is null!");
            return;
          }

          Statistics.INSTANCE.trackEvent(Statistics.EventName.PP_BOOKMARK);
          AlohaHelper.logClick(AlohaHelper.PP_BOOKMARK);
          toggleIsBookmark(mMapObject);
          break;

        case SHARE:
          if (mMapObject == null)
          {
            LOGGER.e(TAG, "A map object cannot be shared, it's null!");
            return;
          }
          Statistics.INSTANCE.trackEvent(Statistics.EventName.PP_SHARE);
          AlohaHelper.logClick(AlohaHelper.PP_SHARE);
          ShareOption.ANY.shareMapObject(getActivity(), mMapObject, mSponsored);
          break;

        case BACK:
          if (mMapObject == null)
          {
            LOGGER.e(TAG, "A mwm request cannot be handled, mMapObject is null!");
            getActivity().finish();
            return;
          }

          if (ParsedMwmRequest.hasRequest())
          {
            ParsedMwmRequest request = ParsedMwmRequest.getCurrentRequest();
            if (ParsedMwmRequest.isPickPointMode())
              request.setPointData(mMapObject.getLat(), mMapObject.getLon(), mMapObject.getTitle(), "");

            request.sendResponseAndFinish(getActivity(), true);
          }
          else
            getActivity().finish();
          break;

        case ROUTE_FROM:
          RoutingController controller = RoutingController.get();
          if (!controller.isPlanning())
          {
            controller.prepare(mMapObject, null);
            hide();
          }
          else if (controller.setStartPoint(mMapObject))
          {
            hide();
          }
          break;

        case ROUTE_TO:
          if (RoutingController.get().isPlanning())
          {
            RoutingController.get().setEndPoint(mMapObject);
            hide();
          }
          else
          {
            getActivity().startLocationToPoint(Statistics.EventName.PP_ROUTE, AlohaHelper.PP_ROUTE,
                                               getMapObject(), true /* canUseMyPositionAsStart */);
          }
          break;

        case ROUTE_ADD:
          if (mMapObject != null)
            RoutingController.get().addStop(mMapObject);
          break;

        case ROUTE_REMOVE:
          if (mMapObject != null)
            RoutingController.get().removeStop(mMapObject);
          break;

        case BOOKING:
        case OPENTABLE:
        case THOR:
          onSponsoredClick(true /* book */, false);
          break;

        case BOOKING_SEARCH:
          if (mMapObject != null && !TextUtils.isEmpty(mMapObject.getBookingSearchUrl()))
          {
            Statistics.INSTANCE.trackBookingSearchEvent(mMapObject);
            Utils.openUrl(getContext(), mMapObject.getBookingSearchUrl());
          }
          break;

        case CALL:
          Utils.callPhone(getContext(), mTvPhone.getText().toString());
          break;
        }
      }
    });

    mDownloaderIcon = new DownloaderStatusIcon(mPreview.findViewById(R.id.downloader_status_frame))
        .setOnIconClickListener(new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            MapManager.warn3gAndDownload(getActivity(), mCurrentCountry.id, new Runnable()
            {
              @Override
              public void run()
              {
                Statistics.INSTANCE.trackEvent(Statistics.EventName.DOWNLOADER_ACTION,
                                               Statistics.params()
                                                         .add(Statistics.EventParam.ACTION, "download")
                                                         .add(Statistics.EventParam.FROM, "placepage")
                                                         .add("is_auto", "false")
                                                         .add("scenario", (mCurrentCountry.isExpandable() ? "download_group"
                                                                                                          : "download")));
              }
            });
          }
        }).setOnCancelClickListener(new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            MapManager.nativeCancel(mCurrentCountry.id);
            Statistics.INSTANCE.trackEvent(Statistics.EventName.DOWNLOADER_CANCEL,
                                           Statistics.params()
                                                     .add(Statistics.EventParam.FROM, "placepage"));
          }
        });

    mDownloaderInfo = (TextView) mPreview.findViewById(R.id.tv__downloader_details);

    mShadowController = new ScrollViewShadowController((ObservableScrollView) mDetails)
        .addBottomShadow()
        .attach();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      setElevation(UiUtils.dimen(R.dimen.placepage_elevation));

    if (UiUtils.isLandscape(getContext()))
      mDetails.setBackgroundResource(0);

    Sponsored.setPriceListener(this);
    Sponsored.setInfoListener(this);
    Viator.setViatorListener(this);
    Cian.setCianListener(this);
  }

  private void initHotelRatingView()
  {
    mHotelReview = findViewById(R.id.ll__place_hotel_rating);
    RecyclerView rvHotelReview = (RecyclerView) findViewById(R.id.rv__place_hotel_review);
    rvHotelReview.setLayoutManager(new LinearLayoutManager(getContext()));
    rvHotelReview.getLayoutManager().setAutoMeasureEnabled(true);
    rvHotelReview.setNestedScrollingEnabled(false);
    rvHotelReview.setHasFixedSize(false);
    rvHotelReview.setAdapter(mReviewAdapter);
    mHotelRating = (TextView) findViewById(R.id.tv__place_hotel_rating);
    mHotelRatingBase = (TextView) findViewById(R.id.tv__place_hotel_rating_base);
    View hotelMoreReviews = findViewById(R.id.tv__place_hotel_reviews_more);
    hotelMoreReviews.setOnClickListener(this);
  }

  private void initHotelNearbyView()
  {
    mHotelNearby = findViewById(R.id.ll__place_hotel_nearby);
    GridView gvHotelNearby = (GridView) findViewById(R.id.gv__place_hotel_nearby);
    gvHotelNearby.setAdapter(mNearbyAdapter);
  }

  private void initHotelGalleryView()
  {
    mHotelGallery = findViewById(R.id.ll__place_hotel_gallery);
    mRvHotelGallery = (RecyclerView) findViewById(
        R.id.rv__place_hotel_gallery);
    mRvHotelGallery.setLayoutManager(new LinearLayoutManager(getContext(),
                                                             LinearLayoutManager.HORIZONTAL, false));
    mRvHotelGallery.addItemDecoration(
        ItemDecoratorFactory.createHotelGalleryDecorator(getContext(), LinearLayoutManager.HORIZONTAL));
    mGalleryAdapter.setListener(this);
    mRvHotelGallery.setAdapter(mGalleryAdapter);
  }

  private void initHotelFacilitiesView()
  {
    mHotelFacilities = findViewById(R.id.ll__place_hotel_facilities);
    RecyclerView rvHotelFacilities = (RecyclerView) findViewById(R.id.rv__place_hotel_facilities);
    rvHotelFacilities.setLayoutManager(new GridLayoutManager(getContext(), 2));
    rvHotelFacilities.getLayoutManager().setAutoMeasureEnabled(true);
    rvHotelFacilities.setNestedScrollingEnabled(false);
    rvHotelFacilities.setHasFixedSize(false);
    mHotelMoreFacilities = findViewById(R.id.tv__place_hotel_facilities_more);
    rvHotelFacilities.setAdapter(mFacilitiesAdapter);
    mHotelMoreFacilities.setOnClickListener(this);
  }

  private void initHotelDescriptionView()
  {
    mHotelDescription = findViewById(R.id.ll__place_hotel_description);
    mTvHotelDescription = (LineCountTextView) findViewById(R.id.tv__place_hotel_details);
    mHotelMoreDescription = findViewById(R.id.tv__place_hotel_more);
    View hotelMoreDescriptionOnWeb = findViewById(R.id.tv__place_hotel_more_on_web);
    mTvHotelDescription.setListener(this);
    mHotelMoreDescription.setOnClickListener(this);
    hotelMoreDescriptionOnWeb.setOnClickListener(this);
  }

  @Override
  public void onPriceReceived(@NonNull String id, @NonNull String price,
                              @NonNull String currencyCode)
  {
    if (mSponsored == null || !TextUtils.equals(id, mSponsored.getId()))
      return;

    String text = Utils.formatCurrencyString(price, currencyCode);

    mSponsoredPrice = getContext().getString(R.string.place_page_starting_from, text);
    if (mMapObject == null)
    {
      LOGGER.e(TAG, "A sponsored info cannot be updated, mMapObject is null!", new Throwable());
      return;
    }
    refreshPreview(mMapObject, NetworkPolicy.newInstance(true));
  }

  @Override
  public void onHotelInfoReceived(@NonNull String id, @NonNull Sponsored.HotelInfo info)
  {
    if (mSponsored == null || !TextUtils.equals(id, mSponsored.getId()))
      return;

    updateHotelDetails(info);
    updateHotelFacilities(info);
    updateHotelGallery(info);
    updateHotelNearby(info);
    updateHotelRating(info);
  }

  private void updateHotelRating(@NonNull Sponsored.HotelInfo info)
  {
    if (info.mReviews == null || info.mReviews.length == 0)
    {
      UiUtils.hide(mHotelReview);
    }
    else
    {
      UiUtils.show(mHotelReview);
      mReviewAdapter.setItems(new ArrayList<>(Arrays.asList(info.mReviews)));
      //noinspection ConstantConditions
      mHotelRating.setText(mSponsored.getRating());
      String text = getResources().getString(R.string.booking_based_on_reviews,
                                             info.mReviewsAmount);
      mHotelRatingBase.setText(text);
      TextView previewReviewCountView = (TextView) mPreviewRatingInfo.findViewById(R.id.tv__review_count);
      previewReviewCountView.setText(text);
    }
  }

  private void updateHotelNearby(@NonNull Sponsored.HotelInfo info)
  {
    if (info.mNearby == null || info.mNearby.length == 0)
    {
      UiUtils.hide(mHotelNearby);
    }
    else
    {
      UiUtils.show(mHotelNearby);
      mNearbyAdapter.setItems(Arrays.asList(info.mNearby));
    }
  }

  private void updateHotelGallery(@NonNull Sponsored.HotelInfo info)
  {
    if (info.mPhotos == null || info.mPhotos.length == 0)
    {
      UiUtils.hide(mHotelGallery);
    }
    else
    {
      UiUtils.show(mHotelGallery);
      mGalleryAdapter.setItems(new ArrayList<>(Arrays.asList(info.mPhotos)));
      mRvHotelGallery.scrollToPosition(0);
    }
  }

  private void updateHotelFacilities(@NonNull Sponsored.HotelInfo info)
  {
    if (info.mFacilities == null || info.mFacilities.length == 0)
    {
      UiUtils.hide(mHotelFacilities);
    }
    else
    {
      UiUtils.show(mHotelFacilities);
      mFacilitiesAdapter.setShowAll(false);
      mFacilitiesAdapter.setItems(Arrays.asList(info.mFacilities));
      mHotelMoreFacilities.setVisibility(info.mFacilities.length > FacilitiesAdapter.MAX_COUNT
                                         ? VISIBLE : GONE);
    }
  }

  private void updateHotelDetails(@NonNull Sponsored.HotelInfo info)
  {
    mTvHotelDescription.setMaxLines(getResources().getInteger(R.integer.pp_hotel_description_lines));
    refreshMetadataOrHide(info.mDescription, mHotelDescription, mTvHotelDescription);
    mHotelMoreDescription.setVisibility(GONE);
  }

  private void clearHotelViews()
  {
    mTvHotelDescription.setText("");
    mHotelMoreDescription.setVisibility(GONE);
    mFacilitiesAdapter.setItems(Collections.<Sponsored.FacilityType>emptyList());
    mHotelMoreFacilities.setVisibility(GONE);
    mGalleryAdapter.setItems(new ArrayList<Image>());
    mNearbyAdapter.setItems(Collections.<Sponsored.NearbyObject>emptyList());
    mReviewAdapter.setItems(new ArrayList<Review>());
    mHotelRating.setText("");
    mHotelRatingBase.setText("");
    mTvSponsoredPrice.setText("");
  }

  private void clearUGCViews()
  {
    if (mUgcController != null)
      mUgcController.clear();
  }

  @Override
  public void onViatorProductsReceived(@NonNull String destId, final @NonNull ViatorProduct[] products)
  {
    if (mSponsored != null)
      updateViatorView(products, mSponsored.getUrl());
  }

  @Override
  public void onRentPlacesReceived(@NonNull RentPlace[] places)
  {
    if (mSponsored != null)
      updateCianView(places, mSponsored.getUrl());
  }

  @Override
  public void onErrorReceived(int errorCode)
  {
    if (mSponsoredAdapter == null || !mSponsoredAdapter.containsLoading())
    {
      mSponsoredAdapter = new CianAdapter("" /* url */ , true /* hasError */, this);
      mRvSponsoredProducts.setAdapter(mSponsoredAdapter);
    }
    else
    {
      mSponsoredAdapter.setLoadingError(Sponsored.TYPE_CIAN,
                                        mSponsored != null ? mSponsored.getUrl() : "");
    }
    Statistics.INSTANCE.trackSponsoredGalleryError(Sponsored.TYPE_CIAN, String.valueOf(errorCode));
  }

  private void initSponsoredGalleryView()
  {
    mSponsoredGalleryView = findViewById(R.id.ll__place_sponsored_gallery);
    mTvSponsoredTitle = (TextView) mSponsoredGalleryView.findViewById(R.id.tv__sponsored_title);
    mIvSponsoredLogo = (ImageView) mSponsoredGalleryView.findViewById(R.id.btn__sponsored_logo);
    mRvSponsoredProducts = (RecyclerView) mSponsoredGalleryView.findViewById(R.id.rv__sponsored_products);
    mRvSponsoredProducts.setLayoutManager(new LinearLayoutManager(getContext(),
                                                                  LinearLayoutManager.HORIZONTAL,
                                                                  false));
    mRvSponsoredProducts.addItemDecoration(
        ItemDecoratorFactory.createSponsoredGalleryDecorator(getContext(), LinearLayoutManager.HORIZONTAL));
    mIvSponsoredLogo.setOnClickListener(this);
  }

  private void updateViatorView(@NonNull final ViatorProduct[] products,
                                @NonNull final String cityUrl)
  {
    if (products.length == 0)
    {
      if (mSponsoredAdapter == null || !mSponsoredAdapter.containsLoading())
      {
        mSponsoredAdapter = new ViatorAdapter(cityUrl, true, this);
        mRvSponsoredProducts.setAdapter(mSponsoredAdapter);
      }
      else
      {
        mSponsoredAdapter.setLoadingError(Sponsored.TYPE_VIATOR, cityUrl);
      }
      Statistics.INSTANCE.trackSponsoredGalleryError(Sponsored.TYPE_VIATOR,
                                                     Statistics.ParamValue.NO_PRODUCTS);
    }
    else
    {
      if (mSponsoredAdapter == null || !mSponsoredAdapter.containsLoading())
      {
        mSponsoredAdapter = new ViatorAdapter(products, cityUrl, this);
        mRvSponsoredProducts.setAdapter(mSponsoredAdapter);
      }
      else
      {
        mRvSponsoredProducts.setItemAnimator(new SingleChangeItemAnimator() {
          @Override
          public void onAnimationFinished()
          {
            mRvSponsoredProducts.setItemAnimator(new DefaultItemAnimator());
            mSponsoredAdapter = new ViatorAdapter(products, cityUrl, PlacePageView.this);
            mRvSponsoredProducts.setAdapter(mSponsoredAdapter);
          }
        });
        mSponsoredAdapter.setLoadingCompleted(Sponsored.TYPE_VIATOR, cityUrl);
      }
    }
  }

  private void updateCianView(@NonNull final RentPlace[] products, @NonNull final String url)
  {
    if (products.length == 0)
    {
      if (mSponsoredAdapter == null || !mSponsoredAdapter.containsLoading())
      {
        mSponsoredAdapter = new CianAdapter(url, true /* hasError */, this);
        mRvSponsoredProducts.setAdapter(mSponsoredAdapter);
      }
      else
      {
        mSponsoredAdapter.setLoadingError(Sponsored.TYPE_CIAN, url);
      }
      Statistics.INSTANCE.trackSponsoredGalleryError(Sponsored.TYPE_CIAN,
                                                     Statistics.ParamValue.NO_PRODUCTS);
    }
    else
    {
      if (mSponsoredAdapter == null || !mSponsoredAdapter.containsLoading())
      {
        mSponsoredAdapter = new CianAdapter(products, url, this);
        mRvSponsoredProducts.setAdapter(mSponsoredAdapter);
      }
      else
      {
        mRvSponsoredProducts.setItemAnimator(new SingleChangeItemAnimator() {
          @Override
          public void onAnimationFinished()
          {
            mRvSponsoredProducts.setItemAnimator(new DefaultItemAnimator());
            mSponsoredAdapter = new CianAdapter(products, url, PlacePageView.this);
            mRvSponsoredProducts.setAdapter(mSponsoredAdapter);
          }
        });
        mSponsoredAdapter.setLoadingCompleted(Sponsored.TYPE_VIATOR, url);
      }
    }
  }

  private void updateGallerySponsoredLogo(@Sponsored.SponsoredType int type)
  {
    if (type != Sponsored.TYPE_VIATOR && type != Sponsored.TYPE_CIAN)
      throw new AssertionError("Unsupported type: " + type);

    int logoAttr = type == Sponsored.TYPE_CIAN ? R.attr.cianLogo : R.attr.viatorLogo;
    TypedArray array = getActivity().getTheme().obtainStyledAttributes(new int[] {logoAttr});
    int attributeResourceId = array.getResourceId(0 /* index */, 0 /* defValue */);
    Drawable drawable = getResources().getDrawable(attributeResourceId);
    array.recycle();
    mIvSponsoredLogo.setImageDrawable(drawable);
  }

  private void showLoadingViatorProducts(@NonNull String id, @NonNull String cityUrl)
  {
    if (!Viator.hasCache(id))
    {
      mSponsoredAdapter = new ViatorAdapter(cityUrl, false, this);
      mRvSponsoredProducts.setAdapter(mSponsoredAdapter);
    }
  }

  private void showLoadingCianProducts(@NonNull FeatureId id, @NonNull String url)
  {
    if (!Cian.hasCache(id))
    {
      mSponsoredAdapter = new CianAdapter(url, false /* hasError */, this);
      mRvSponsoredProducts.setAdapter(mSponsoredAdapter);
    }
  }

  private void updateGallerySponsoredTitle(@Sponsored.SponsoredType int type)
  {
    mTvSponsoredTitle.setText(type == Sponsored.TYPE_CIAN ? R.string.subtitle_rent
                                                          : R.string.place_page_viator_title);
  }

  private void hideSponsoredGalleryViews()
  {
    UiUtils.hide(mSponsoredGalleryView);
  }

  private void clearSponsoredGalleryViews()
  {
    mSponsoredAdapter = null;
    mRvSponsoredProducts.swapAdapter(null /* adapter */, false /* removeAndRecycleExistingViews */);
  }

  @Override
  public void onItemSelected(@NonNull String url, @Sponsored.SponsoredType int type)
  {
    Utils.openUrl(getContext(), url);
    Statistics.INSTANCE.trackSponsoredEvent(Statistics.EventName.PP_SPONSOR_ITEM_SELECTED,
                                            type);
  }

  @Override
  public void onMoreItemSelected(@NonNull String url, @Sponsored.SponsoredType int type)
  {
    Utils.openUrl(getContext(), url);
    Statistics.INSTANCE.trackSponsoredEvent(Statistics.EventName.PP_SPONSOR_MORE_SELECTED,
                                            type);
  }

  @Override
  public void onLineCountCalculated(boolean grater)
  {
    UiUtils.showIf(grater, mHotelMoreDescription);
  }

  @Override
  public void onItemClick(View v, int position)
  {
    if (mMapObject == null || mSponsored == null)
    {
      LOGGER.e(TAG, "A photo gallery cannot be started, mMapObject/mSponsored is null!");
      return;
    }

    Statistics.INSTANCE.trackHotelEvent(PP_HOTEL_GALLERY_OPEN, mSponsored, mMapObject);

    if (position == GalleryAdapter.MAX_COUNT - 1
        && mGalleryAdapter.getItems().size() > GalleryAdapter.MAX_COUNT)
    {
      GalleryActivity.start(getContext(), mGalleryAdapter.getItems(), mMapObject.getTitle());
    }
    else
    {
      FullScreenGalleryActivity.start(getContext(), mGalleryAdapter.getItems(), position);
    }
  }

  @Override
  public void onItemClick(@NonNull Sponsored.NearbyObject item)
  {
//  TODO go to selected object on map
  }

  private void onSponsoredClick(final boolean book, final boolean isMoreDetails)
  {
    Utils.checkConnection(
        getActivity(), R.string.common_check_internet_connection_dialog, new Utils.Proc<Boolean>()
        {
          @Override
          public void invoke(@NonNull Boolean result)
          {
            if (!result)
              return;

            Sponsored info = mSponsored;
            if (info == null)
              return;

            switch (info.getType())
            {
              case Sponsored.TYPE_BOOKING:
                if (mMapObject == null)
                  break;

                if (book)
                {
                  Statistics.INSTANCE.trackBookHotelEvent(info, mMapObject);
                }
                else
                {
                  String event = isMoreDetails ? PP_SPONSORED_DETAILS : PP_HOTEL_DESCRIPTION_LAND;
                  Statistics.INSTANCE.trackHotelEvent(event, info, mMapObject);
                }
                break;
              case Sponsored.TYPE_GEOCHAT:
                break;
              case Sponsored.TYPE_OPENTABLE:
                if (mMapObject != null)
                  Statistics.INSTANCE.trackRestaurantEvent(PP_SPONSORED_OPENTABLE, info, mMapObject);
                break;
              case Sponsored.TYPE_THOR:
                if (mMapObject != null)
                  Statistics.INSTANCE.trackSponsoredObjectEvent(PP_SPONSORED_ACTION, info, mMapObject);
                break;
              case Sponsored.TYPE_NONE:
                break;
            }

            try
            {
              Utils.openUrl(getContext(), book ? info.getUrl() : info.getDescriptionUrl());
            }
            catch (ActivityNotFoundException e)
            {
              LOGGER.e(TAG, "Failed to handle click on sponsored: ", e);
              AlohaHelper.logException(e);
            }
          }
        });
  }

  @Override
  public void onNeedOpenBanner()
  {
    if (mBannerController != null)
    {
      if (!mBannerController.isOpened())
        compensateViewHeight(0);
      mBannerController.open();
    }
  }

  private void compensateViewHeight(int height)
  {
    if (mHeightCompensationView == null)
      return;

    ViewGroup.LayoutParams lp = mHeightCompensationView.getLayoutParams();
    lp.height = height;
    mHeightCompensationView.setLayoutParams(lp);
  }

  private void init(AttributeSet attrs, int defStyleAttr)
  {
    initViews();

    if (isInEditMode())
      return;

    final TypedArray attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.PlacePageView, defStyleAttr, 0);
    final int animationType = attrArray.getInt(R.styleable.PlacePageView_animationType, 0);
    mIsDocked = attrArray.getBoolean(R.styleable.PlacePageView_docked, false);
    mIsFloating = attrArray.getBoolean(R.styleable.PlacePageView_floating, false);
    attrArray.recycle();

    boolean isBottom = animationType == 0;
    mAnimationController = isBottom ? new BottomPlacePageAnimationController(this)
                                    : new LeftPlacePageAnimationController(this);
    if (isBottom)
      ((BottomPlacePageAnimationController) mAnimationController).setBannerOpenListener(this);
  }

  public void restore()
  {
    if (mMapObject == null)
    {
      // FIXME query map object again
      LOGGER.e(TAG, "A place page cannot be restored, mMapObject is null!", new Throwable());
      return;
    }

    setMapObject(mMapObject, true, null);
  }

  @Override
  public boolean onTouchEvent(@NonNull MotionEvent event)
  {
    return mAnimationController.onTouchEvent(event);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event)
  {
    return mAnimationController.onInterceptTouchEvent(event);
  }

  public boolean isDocked()
  {
    return mIsDocked;
  }

  public boolean isFloating()
  {
    return mIsFloating;
  }

  public State getState()
  {
    return mAnimationController.getState();
  }

  public void setState(State state)
  {
    mDetails.scrollTo(0, 0);

    // If place page is closed the state of webview should be cleared,
    // otherwise the animation controller will work with stale webview height,
    // since the webview caches its state completely, and animation will work wrong.
    if (state == State.HIDDEN)
      clearBookmarkWebView();

    int heightCompensation = 0;
    if (mBannerController != null)
    {
      State lastState = getState();
      boolean isLastStateNotHiddenOrPreview = lastState != State.HIDDEN
                                              && lastState != State.PREVIEW;
      if (isLastStateNotHiddenOrPreview && (state == State.HIDDEN || state == State.PREVIEW)
          && !UiUtils.isLandscape(getContext()))
      {
        if (mBannerController.close())
          heightCompensation = mBannerController.getLastBannerHeight();
      }
      else if (isLastStateNotHiddenOrPreview)
      {
        mBannerController.open();
      }
    }

    compensateViewHeight(heightCompensation);

    if (mMapObject != null)
      mAnimationController.setState(state, mMapObject.getMapObjectType());

    if (!mIsDocked && !mIsFloating)
    {
      // After ninepatch background is set from code, all paddings are lost, so we need to restore it later.
      int bottom = mBannerController != null && mBannerController.isBannerVisible()
                   ? 0 : (int) getResources().getDimension(R.dimen.margin_base);
      int left = mPreview.getPaddingLeft();
      int right = mPreview.getPaddingRight();
      int top = mPreview.getPaddingTop();
      mPreview.setBackgroundResource(ThemeUtils.getResource(getContext(), state == State.PREVIEW ? R.attr.ppPreviewHeadClosed
                                                                                                 : R.attr.ppPreviewHeadOpen));
      mPreview.setPadding(left, top, right, bottom);
    }
  }

  private void clearBookmarkWebView()
  {
    mWvBookmarkNote.loadUrl("about:blank");
  }

  @Nullable
  public MapObject getMapObject()
  {
    return mMapObject;
  }

  /**
   * @param mapObject new MapObject
   * @param force     if true, new object'll be set without comparison with the old one
   * @param listener  listener
   */
  public void setMapObject(@Nullable MapObject mapObject, boolean force,
                           @Nullable final SetMapObjectListener listener)
  {
    if (!force && MapObject.same(mMapObject, mapObject))
    {
      if (listener != null)
        listener.onSetMapObjectComplete();
      return;
    }

    mMapObject = mapObject;
    mSponsored = (mMapObject == null ? null : Sponsored.nativeGetCurrent());

    if (isNetworkNeeded())
    {
      NetworkPolicy.checkNetworkPolicy(getActivity().getSupportFragmentManager(),
                                       new NetworkPolicy.NetworkPolicyListener()
      {
        @Override
        public void onResult(@NonNull NetworkPolicy policy)
        {
          setMapObjectInternal(policy);
          if (listener != null)
            listener.onSetMapObjectComplete();
        }
      });
    }
    else
    {
      setMapObjectInternal(NetworkPolicy.newInstance(false));
      if (listener != null)
        listener.onSetMapObjectComplete();
    }
  }

  private void setMapObjectInternal(@NonNull NetworkPolicy policy)
  {
    detachCountry();
    if (mMapObject != null)
    {
      clearHotelViews();
      clearSponsoredGalleryViews();
      clearUGCViews();
      processSponsored(policy);
      if (mUgcController != null)
        mUgcController.getUGC(mMapObject);

      String country = MapManager.nativeGetSelectedCountry();
      if (country != null && !RoutingController.get().isNavigating())
        attachCountry(country);
    }

    refreshViews(policy);
  }

  private void processSponsored(@NonNull NetworkPolicy policy)
  {
    if (mSponsored == null || mMapObject == null)
      return;

    mSponsored.updateId(mMapObject);
    mSponsoredPrice = mSponsored.getPrice();

    String currencyCode = Utils.getCurrencyCode();

    if (mSponsored.getId() == null || TextUtils.isEmpty(currencyCode))
      return;

    if (mSponsored.getType() == Sponsored.TYPE_BOOKING)
    {
      Sponsored.requestPrice(mSponsored.getId(), currencyCode, policy);
      Sponsored.requestInfo(mSponsored, Locale.getDefault().toString(), policy);
      return;
    }

    boolean isViator = mSponsored.getType() == Sponsored.TYPE_VIATOR;
    boolean isCian = mSponsored.getType() == Sponsored.TYPE_CIAN;

    if (!isViator && !isCian)
      return;

    updateGallerySponsoredLogo(mSponsored.getType());
    updateGallerySponsoredTitle(mSponsored.getType());
    UiUtils.show(mSponsoredGalleryView);

    boolean hasInCache = isCian ? Cian.hasCache(mMapObject.getFeatureId())
                                : Viator.hasCache(mSponsored.getId());
    final String url = !TextUtils.isEmpty(mSponsored.getUrl()) ? mSponsored.getUrl()
                                                               : mSponsored.getDescriptionUrl();
    if (!ConnectionState.isConnected() && !hasInCache)
    {
      if (isCian)
      {
        updateCianView(new RentPlace[]{}, url);
        return;
      }

      updateViatorView(new ViatorProduct[]{}, url);
      return;
    }

    if (isViator)
    {
      showLoadingViatorProducts(mSponsored.getId(), url);
      Viator.requestViatorProducts(policy, mSponsored.getId(), currencyCode);
      return;
    }

    showLoadingCianProducts(mMapObject.getFeatureId(), url);
    Cian.getRentNearby(policy, mMapObject.getLat(), mMapObject.getLon(), mMapObject.getFeatureId());
  }

  private boolean isNetworkNeeded()
  {
    return mMapObject != null && (isSponsored() || mMapObject.getBanners() != null);
  }

  private void refreshViews(@NonNull NetworkPolicy policy)
  {
    if (mMapObject == null)
    {
      LOGGER.e(TAG, "A place page views cannot be refreshed, mMapObject is null", new Throwable());
      return;
    }

    refreshPreview(mMapObject, policy);
    refreshViewsInternal(mMapObject);
  }

  public void refreshViews()
  {
    if (mMapObject == null)
    {
      LOGGER.e(TAG, "A place page views cannot be refreshed, mMapObject is null", new Throwable());
      return;
    }

    refreshPreview(mMapObject);
    refreshViewsInternal(mMapObject);
  }

  private void refreshViewsInternal(@NonNull MapObject mapObject)
  {
    refreshDetails(mapObject);

    final Location loc = LocationHelper.INSTANCE.getSavedLocation();

    switch (mapObject.getMapObjectType())
    {
      case MapObject.BOOKMARK:
        refreshDistanceToObject(mapObject, loc);
        showBookmarkDetails(mapObject);
        updateBookmarkButton();
        setButtons(mapObject, false, true);
        break;
      case MapObject.POI:
      case MapObject.SEARCH:
        refreshDistanceToObject(mapObject, loc);
        hideBookmarkDetails();
        setButtons(mapObject, false, true);
        break;
      case MapObject.API_POINT:
        refreshDistanceToObject(mapObject, loc);
        hideBookmarkDetails();
        setButtons(mapObject, true, true);
        break;
      case MapObject.MY_POSITION:
        refreshMyPosition(mapObject, loc);
        hideBookmarkDetails();
        setButtons(mapObject, false, false);
        break;
    }

    UiThread.runLater(new Runnable()
    {
      @Override
      public void run()
      {
        mShadowController.updateShadows();
        mPreview.requestLayout();
      }
    });
  }

  private void colorizeSubtitle()
  {
    String text = mTvSubtitle.getText().toString();
    if (TextUtils.isEmpty(text))
      return;

    int start = text.indexOf("★");
    if (start > -1)
    {
      SpannableStringBuilder sb = new SpannableStringBuilder(text);
      sb.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.base_yellow)),
                 start, sb.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

      mTvSubtitle.setText(sb);
    }
  }

  private void refreshPreview(@NonNull MapObject mapObject, @NonNull NetworkPolicy policy)
  {
    if (mBannerController != null)
    {
      boolean canShow = mapObject.getMapObjectType() != MapObject.MY_POSITION
                        && policy.сanUseNetwork();
      mBannerController.updateData(canShow ? mapObject.getBanners() : null);
    }

    refreshPreview(mapObject);
  }

  private void refreshPreview(@NonNull MapObject mapObject)
  {
    UiUtils.setTextAndHideIfEmpty(mTvTitle, mapObject.getTitle());
    UiUtils.setTextAndHideIfEmpty(mTvSecondaryTitle, mapObject.getSecondaryTitle());
    if (mToolbar != null)
      mToolbar.setTitle(mapObject.getTitle());
    UiUtils.setTextAndHideIfEmpty(mTvSubtitle, mapObject.getSubtitle());
    colorizeSubtitle();
    UiUtils.hide(mAvDirection);
    UiUtils.setTextAndHideIfEmpty(mTvAddress, mapObject.getAddress());
    boolean sponsored = isSponsored();
    UiUtils.showIf(sponsored, mPreviewRatingInfo);
    if (sponsored)
    {
      boolean isPriceEmpty = TextUtils.isEmpty(mSponsoredPrice);
      @SuppressWarnings("ConstantConditions")
      boolean isRatingEmpty = TextUtils.isEmpty(mSponsored.getRating());
      Impress impress = Impress.values()[mSponsored.getImpress()];
      mRatingView.setRating(impress, mSponsored.getRating());
      UiUtils.showIf(!isRatingEmpty, mRatingView);
      mTvSponsoredPrice.setText(mSponsoredPrice);
      UiUtils.showIf(!isPriceEmpty, mTvSponsoredPrice);
      UiUtils.showIf((!isRatingEmpty || !isPriceEmpty) &&
                     mSponsored.getType() == Sponsored.TYPE_BOOKING, mPreviewRatingInfo);
    }
  }

  private boolean isSponsored()
  {
    return mSponsored != null && mSponsored.getType() != Sponsored.TYPE_NONE;
  }

  @Nullable
  public Sponsored getSponsored()
  {
    return mSponsored;
  }

  private void refreshDetails(@NonNull MapObject mapObject)
  {
    refreshLatLon(mapObject);

    mGalleryAdapter.setItems(new ArrayList<Image>());
    if (mSponsored == null)
    {
      final String website = mapObject.getMetadata(Metadata.MetadataType.FMD_WEBSITE);
      refreshMetadataOrHide(TextUtils.isEmpty(website) ? mapObject.getMetadata(Metadata.MetadataType.FMD_URL)
                                                       : website, mWebsite, mTvWebsite);
      hideHotelViews();
      hideSponsoredGalleryViews();
    }
    else
    {
      UiUtils.hide(mWebsite);
      UiUtils.show(mHotelMore);

      if (mSponsored.getType() != Sponsored.TYPE_BOOKING)
        hideHotelViews();
      if (mSponsored.getType() != Sponsored.TYPE_VIATOR
          && mSponsored.getType() != Sponsored.TYPE_CIAN)
      {
        hideSponsoredGalleryViews();
      }
    }

    refreshMetadataOrHide(mapObject.getMetadata(Metadata.MetadataType.FMD_PHONE_NUMBER), mPhone, mTvPhone);
    refreshMetadataOrHide(mapObject.getMetadata(Metadata.MetadataType.FMD_EMAIL), mEmail, mTvEmail);
    refreshMetadataOrHide(mapObject.getMetadata(Metadata.MetadataType.FMD_OPERATOR), mOperator, mTvOperator);
    refreshMetadataOrHide(Framework.nativeGetActiveObjectFormattedCuisine(), mCuisine, mTvCuisine);
    refreshMetadataOrHide(mapObject.getMetadata(Metadata.MetadataType.FMD_WIKIPEDIA), mWiki, null);
    refreshMetadataOrHide(mapObject.getMetadata(Metadata.MetadataType.FMD_INTERNET), mWifi, null);
    refreshMetadataOrHide(mapObject.getMetadata(Metadata.MetadataType.FMD_FLATS), mEntrance, mTvEntrance);
    refreshOpeningHours(mapObject);

    showTaxiOffer(mapObject);

    boolean inRouting = RoutingController.get().isNavigating() ||
                        RoutingController.get().isPlanning();

    if (inRouting || MapManager.nativeIsLegacyMode())
    {
      UiUtils.hide(mEditPlace, mAddOrganisation, mAddPlace, mLocalAd, mEditTopSpace);
    }
    else
    {
      UiUtils.showIf(Editor.nativeShouldShowEditPlace(), mEditPlace);
      UiUtils.showIf(Editor.nativeShouldShowAddBusiness(), mAddOrganisation);
      UiUtils.showIf(Editor.nativeShouldShowAddPlace(), mAddPlace);
      UiUtils.showIf(UiUtils.isVisible(mEditPlace)
                     || UiUtils.isVisible(mAddOrganisation)
                     || UiUtils.isVisible(mAddPlace), mEditTopSpace);
      refreshLocalAdInfo(mapObject);
    }
  }

  private void showTaxiOffer(@NonNull MapObject mapObject)
  {
    List<Integer> taxiTypes = mapObject.getReachableByTaxiTypes();

    boolean showTaxiOffer = taxiTypes != null && !taxiTypes.isEmpty() &&
                            LocationHelper.INSTANCE.getMyPosition() != null &&
                            ConnectionState.isConnected();
    UiUtils.showIf(showTaxiOffer, mTaxi, mTaxiShadow, mTaxiDivider);

    if (!showTaxiOffer)
      return;

    // At this moment we display only a one taxi provider at the same time.
    @TaxiManager.TaxiType
    int type = taxiTypes.get(0);
    UiUtils.showTaxiIcon((ImageView) mTaxi.findViewById(R.id.iv__place_page_taxi), type);
    UiUtils.showTaxiTitle((TextView) mTaxi.findViewById(R.id.tv__place_page_taxi), type);
    Statistics.INSTANCE.trackTaxiEvent(Statistics.EventName.ROUTING_TAXI_SHOW_IN_PP, type);
  }

  private void hideHotelViews()
  {
    UiUtils.hide(mHotelDescription, mHotelFacilities, mHotelGallery, mHotelNearby,
                 mHotelReview, mHotelMore);
  }

  private void refreshLocalAdInfo(@NonNull MapObject mapObject)
  {
    LocalAdInfo localAdInfo = mapObject.getLocalAdInfo();
    boolean isLocalAdAvailable = localAdInfo != null && localAdInfo.isAvailable();
    if (isLocalAdAvailable && !TextUtils.isEmpty(localAdInfo.getUrl()))
    {
      mTvLocalAd.setText(localAdInfo.isCustomer() ? R.string.view_campaign_button
                                                  : R.string.create_campaign_button);
      UiUtils.show(mLocalAd);
    }
    else
    {
      UiUtils.hide(mLocalAd);
    }
  }

  private void refreshOpeningHours(@NonNull MapObject mapObject)
  {
    final Timetable[] timetables = OpeningHours.nativeTimetablesFromString(mapObject.getMetadata(Metadata.MetadataType.FMD_OPEN_HOURS));
    if (timetables == null || timetables.length == 0)
    {
      UiUtils.hide(mOpeningHours);
      return;
    }

    UiUtils.show(mOpeningHours);

    final Resources resources = getResources();
    if (timetables[0].isFullWeek())
    {
      refreshTodayOpeningHours((timetables[0].isFullday ? resources.getString(R.string.twentyfour_seven)
                                                        : resources.getString(R.string.daily) + " " + timetables[0].workingTimespan),
                               ThemeUtils.getColor(getContext(), android.R.attr.textColorPrimary));
      UiUtils.hide(mFullOpeningHours);
      return;
    }

    boolean containsCurrentWeekday = false;
    final int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
    for (Timetable tt : timetables)
    {
      if (tt.containsWeekday(currentDay))
      {
        containsCurrentWeekday = true;
        String workingTime;

        if (tt.isFullday)
        {
          String allDay = resources.getString(R.string.editor_time_allday);
          workingTime = Utils.unCapitalize(allDay);
        }
        else
        {
          workingTime = tt.workingTimespan.toString();
        }

        refreshTodayOpeningHours(resources.getString(R.string.today) + " " + workingTime,
                                 ThemeUtils.getColor(getContext(), android.R.attr.textColorPrimary));

        break;
      }
    }

    UiUtils.setTextAndShow(mFullOpeningHours, TimeFormatUtils.formatTimetables(timetables));
    if (!containsCurrentWeekday)
      refreshTodayOpeningHours(resources.getString(R.string.day_off_today), resources.getColor(R.color.base_red));
  }

  private void refreshTodayOpeningHours(String text, @ColorInt int color)
  {
    UiUtils.setTextAndShow(mTodayOpeningHours, text);
    mTodayOpeningHours.setTextColor(color);
  }

  private void updateBookmarkButton()
  {
    if (mBookmarkButtonIcon == null)
      return;

    if (mBookmarkSet)
      mBookmarkButtonIcon.setImageResource(R.drawable.ic_bookmarks_on);
    else
      mBookmarkButtonIcon.setImageDrawable(Graphics.tint(getContext(), R.drawable.ic_bookmarks_off, R.attr.iconTint));
  }

  private void hideBookmarkDetails()
  {
    mBookmarkSet = false;
    UiUtils.hide(mBookmarkFrame);
    updateBookmarkButton();
  }

  private void showBookmarkDetails(@NonNull MapObject mapObject)
  {
    mBookmarkSet = true;
    UiUtils.show(mBookmarkFrame);

    final String notes = ((Bookmark) mapObject).getBookmarkDescription();

    if (TextUtils.isEmpty(notes))
    {
      UiUtils.hide(mTvBookmarkNote, mWvBookmarkNote);
      return;
    }

    if (StringUtils.nativeIsHtml(notes))
    {
      mWvBookmarkNote.loadData(notes, "text/html; charset=utf-8", null);
      UiUtils.show(mWvBookmarkNote);
      UiUtils.hide(mTvBookmarkNote);
    }
    else
    {
      mTvBookmarkNote.setText(notes);
      Linkify.addLinks(mTvBookmarkNote, Linkify.ALL);
      UiUtils.show(mTvBookmarkNote);
      UiUtils.hide(mWvBookmarkNote);
    }
  }

  private void setButtons(@NonNull MapObject mapObject, boolean showBackButton, boolean showRoutingButton)
  {
    List<PlacePageButtons.Item> buttons = new ArrayList<>();
    if (RoutingController.get().isRoutePoint(mapObject))
    {
      buttons.add(PlacePageButtons.Item.ROUTE_REMOVE);
      mButtons.setItems(buttons);
      return;
    }

    if (showBackButton || ParsedMwmRequest.isPickPointMode())
      buttons.add(PlacePageButtons.Item.BACK);

    if (mSponsored != null)
    {
      switch (mSponsored.getType())
      {
        case Sponsored.TYPE_BOOKING:
          buttons.add(PlacePageButtons.Item.BOOKING);
          break;
        case Sponsored.TYPE_GEOCHAT:
          break;
        case Sponsored.TYPE_OPENTABLE:
          buttons.add(PlacePageButtons.Item.OPENTABLE);
          break;
        case Sponsored.TYPE_THOR:
          buttons.add(PlacePageButtons.Item.THOR);
          break;
        case Sponsored.TYPE_NONE:
          break;
      }
    }

    if (!TextUtils.isEmpty(mapObject.getBookingSearchUrl()))
      buttons.add(PlacePageButtons.Item.BOOKING_SEARCH);

    if (mapObject.hasPhoneNumber())
      buttons.add(PlacePageButtons.Item.CALL);

    buttons.add(PlacePageButtons.Item.BOOKMARK);

    if (RoutingController.get().isPlanning() || showRoutingButton)
    {
      buttons.add(PlacePageButtons.Item.ROUTE_FROM);
      buttons.add(PlacePageButtons.Item.ROUTE_TO);
      if (RoutingController.get().isStopPointAllowed())
        buttons.add(PlacePageButtons.Item.ROUTE_ADD);
    }

    buttons.add(PlacePageButtons.Item.SHARE);

    mButtons.setItems(buttons);
  }

  public void refreshLocation(Location l)
  {
    if (mMapObject == null)
    {
      LOGGER.e(TAG, "A location cannot be refreshed, mMapObject is null!", new Throwable());
      return;
    }

    if (MapObject.isOfType(MapObject.MY_POSITION, mMapObject))
      refreshMyPosition(mMapObject, l);
    else
      refreshDistanceToObject(mMapObject, l);
  }

  private void refreshMyPosition(@NonNull MapObject mapObject, Location l)
  {
    UiUtils.hide(mTvDistance);

    if (l == null)
      return;

    final StringBuilder builder = new StringBuilder();
    if (l.hasAltitude())
    {
      double altitude = l.getAltitude();
      builder.append(altitude >= 0 ? "▲" : "▼");
      builder.append(Framework.nativeFormatAltitude(altitude));
    }
    if (l.hasSpeed())
      builder.append("   ")
             .append(Framework.nativeFormatSpeed(l.getSpeed()));
    UiUtils.setTextAndHideIfEmpty(mTvSubtitle, builder.toString());

    mapObject.setLat(l.getLatitude());
    mapObject.setLon(l.getLongitude());
    refreshLatLon(mapObject);
  }

  private void refreshDistanceToObject(@NonNull MapObject mapObject, Location l)
  {
    UiUtils.showIf(l != null, mTvDistance);
    if (l == null)
      return;

    mTvDistance.setVisibility(View.VISIBLE);
    double lat = mapObject.getLat();
    double lon = mapObject.getLon();
    DistanceAndAzimut distanceAndAzimuth =
        Framework.nativeGetDistanceAndAzimuthFromLatLon(lat, lon, l.getLatitude(), l.getLongitude(), 0.0);
    mTvDistance.setText(distanceAndAzimuth.getDistance());
  }

  private void refreshLatLon(@NonNull MapObject mapObject)
  {
    final double lat = mapObject.getLat();
    final double lon = mapObject.getLon();
    final String[] latLon = Framework.nativeFormatLatLonToArr(lat, lon, mIsLatLonDms);
    if (latLon.length == 2)
      mTvLatlon.setText(String.format(Locale.US, "%1$s, %2$s", latLon[0], latLon[1]));
  }

  private static void refreshMetadataOrHide(String metadata, View metaLayout, TextView metaTv)
  {
    if (!TextUtils.isEmpty(metadata))
    {
      metaLayout.setVisibility(View.VISIBLE);
      if (metaTv != null)
        metaTv.setText(metadata);
    }
    else
      metaLayout.setVisibility(View.GONE);
  }

  public void refreshAzimuth(double northAzimuth)
  {
    if (isHidden() ||
        mMapObject == null ||
        MapObject.isOfType(MapObject.MY_POSITION, mMapObject))
      return;

    final Location location = LocationHelper.INSTANCE.getSavedLocation();
    if (location == null)
      return;

    final double azimuth = Framework.nativeGetDistanceAndAzimuthFromLatLon(mMapObject.getLat(), mMapObject
                                                                               .getLon(),
                                                                           location.getLatitude(), location
                                                                               .getLongitude(),
                                                                           northAzimuth)
                                    .getAzimuth();
    if (azimuth >= 0)
    {
      UiUtils.show(mAvDirection);
      mAvDirection.setAzimuth(azimuth);
    }
  }

  public void setOnVisibilityChangedListener(BasePlacePageAnimationController.OnVisibilityChangedListener listener)
  {
    mAnimationController.setOnVisibilityChangedListener(new PlacePageVisibilityProxy(listener, mBannerController));
  }

  public void setOnAnimationListener(@Nullable BasePlacePageAnimationController.OnAnimationListener listener)
  {
    mAnimationController.setOnProgressListener(listener);
  }

  private void addOrganisation()
  {
    Statistics.INSTANCE.trackEvent(Statistics.EventName.EDITOR_ADD_CLICK,
                                   Statistics.params()
                                             .add(Statistics.EventParam.FROM, "placepage"));
    getActivity().showPositionChooser(true, false);
  }

  private void addPlace()
  {
    // TODO add statistics
    getActivity().showPositionChooser(false, true);
  }

  @Override
  public void onClick(View v)
  {
    switch (v.getId())
    {
      case R.id.ll__place_editor:
        getActivity().showEditor();
        break;
      case R.id.ll__add_organisation:
        addOrganisation();
        break;
      case R.id.ll__place_add:
        addPlace();
        break;
      case R.id.ll__local_ad:
        if (mMapObject != null)
        {
          LocalAdInfo localAdInfo = mMapObject.getLocalAdInfo();
          if (localAdInfo == null)
            throw new AssertionError("A local ad must be non-null if button is shown!");

          if (!TextUtils.isEmpty(localAdInfo.getUrl()))
          {
            Statistics.INSTANCE.trackPPOwnershipButtonClick(mMapObject);
            Utils.openUrl(getContext(), localAdInfo.getUrl());
          }
        }
        break;
      case R.id.ll__more:
        onSponsoredClick(false /* book */, true /* isMoreDetails */);
        break;
      case R.id.tv__place_hotel_more_on_web:
        onSponsoredClick(false /* book */, false /* isMoreDetails */);
        break;
      case R.id.ll__place_latlon:
        mIsLatLonDms = !mIsLatLonDms;
        MwmApplication.prefs().edit().putBoolean(PREF_USE_DMS, mIsLatLonDms).apply();
        if (mMapObject == null)
        {
          LOGGER.e(TAG, "A LatLon cannot be refreshed, mMapObject is null");
          break;
        }
        refreshLatLon(mMapObject);
        break;
      case R.id.ll__place_phone:
        Utils.callPhone(getContext(), mTvPhone.getText().toString());
        if (mMapObject != null)
          Framework.logLocalAdsEvent(Framework.LOCAL_ADS_EVENT_CLICKED_PHONE, mMapObject);
        break;
      case R.id.ll__place_website:
        Utils.openUrl(getContext(), mTvWebsite.getText().toString());
        if (mMapObject != null)
          Framework.logLocalAdsEvent(Framework.LOCAL_ADS_EVENT_CLICKED_WEBSITE, mMapObject);
        break;
      case R.id.ll__place_wiki:
        // TODO: Refactor and use separate getters for Wiki and all other PP meta info too.
        if (mMapObject == null)
        {
          LOGGER.e(TAG, "Cannot follow url, mMapObject is null!", new Throwable());
          break;
        }
        Utils.openUrl(getContext(), mMapObject.getMetadata(Metadata.MetadataType.FMD_WIKIPEDIA));
        break;
      case R.id.direction_frame:
        Statistics.INSTANCE.trackEvent(Statistics.EventName.PP_DIRECTION_ARROW);
        AlohaHelper.logClick(AlohaHelper.PP_DIRECTION_ARROW);
        showBigDirection();
        break;
      case R.id.ll__place_email:
        Utils.sendTo(getContext(), mTvEmail.getText().toString());
        break;
      case R.id.tv__bookmark_edit:
        if (mMapObject == null)
        {
          LOGGER.e(TAG, "A bookmark cannot be edited, mMapObject is null!", new Throwable());
          return;
        }
        Bookmark bookmark = (Bookmark) mMapObject;
        EditBookmarkFragment.editBookmark(bookmark.getCategoryId(), bookmark.getBookmarkId(),
                                          getActivity(), getActivity().getSupportFragmentManager(),
                                          this);
        break;
      case R.id.tv__place_hotel_more:
        UiUtils.hide(mHotelMoreDescription);
        mTvHotelDescription.setMaxLines(Integer.MAX_VALUE);
        break;
      case R.id.tv__place_hotel_facilities_more:
        if (mSponsored != null && mMapObject != null)
          Statistics.INSTANCE.trackHotelEvent(PP_HOTEL_FACILITIES, mSponsored, mMapObject);
        UiUtils.hide(mHotelMoreFacilities);
        mFacilitiesAdapter.setShowAll(true);
        break;
      case R.id.tv__place_hotel_reviews_more:
        if (isSponsored())
        {
          //null checking is done in 'isSponsored' method
          //noinspection ConstantConditions
          Utils.openUrl(getContext(), mSponsored.getReviewUrl());
          if (mMapObject != null)
            Statistics.INSTANCE.trackHotelEvent(PP_HOTEL_REVIEWS_LAND, mSponsored, mMapObject);
        }
        break;
      case R.id.tv__place_page_order_taxi:
        RoutingController.get().prepare(LocationHelper.INSTANCE.getMyPosition(), mMapObject,
                                        Framework.ROUTER_TYPE_TAXI);
        hide();
        Framework.nativeDeactivatePopup();
        if (mMapObject != null)
        {
          List<Integer> types = mMapObject.getReachableByTaxiTypes();
          if (types != null && !types.isEmpty())
          {
            @TaxiManager.TaxiType
            int type = types.get(0);
            Statistics.INSTANCE.trackTaxiEvent(Statistics.EventName.ROUTING_TAXI_CLICK_IN_PP, type);
          }
        }
        break;
      case R.id.btn__sponsored_logo:
        if (mSponsored == null)
          break;

        String url = !TextUtils.isEmpty(mSponsored.getUrl()) ? mSponsored.getUrl()
                                                             : mSponsored.getDescriptionUrl();
        if (!TextUtils.isEmpty(url))
        {
          Utils.openUrl(getContext(), url);
          Statistics.INSTANCE.trackSponsoredEvent(Statistics.EventName.PP_SPONSOR_LOGO_SELECTED,
                                                  mSponsored.getType());
        }
        break;
    }
  }

  private void toggleIsBookmark(@NonNull MapObject mapObject)
  {
    if (MapObject.isOfType(MapObject.BOOKMARK, mapObject))
      setMapObject(Framework.nativeDeleteBookmarkFromMapObject(), true, null);
    else
      setMapObject(BookmarkManager.INSTANCE.addNewBookmark(BookmarkManager.nativeFormatNewBookmarkName(),
                                                           mapObject.getLat(), mapObject.getLon()), true, null);
    post(new Runnable()
    {
      @Override
      public void run()
      {
        setState(mBookmarkSet ? State.DETAILS : State.PREVIEW);
      }
    });
  }

  private void showBigDirection()
  {
    final DirectionFragment fragment = (DirectionFragment) Fragment.instantiate(getActivity(), DirectionFragment.class
        .getName(), null);
    fragment.setMapObject(mMapObject);
    fragment.show(getActivity().getSupportFragmentManager(), null);
  }

  @Override
  public boolean onLongClick(View v)
  {
    final Object tag = v.getTag();
    final String tagStr = tag == null ? "" : tag.toString();
    AlohaHelper.logLongClick(tagStr);

    final PopupMenu popup = new PopupMenu(getContext(), v);
    final Menu menu = popup.getMenu();
    final List<String> items = new ArrayList<>();
    switch (v.getId())
    {
      case R.id.ll__place_latlon:
        if (mMapObject == null)
        {
          LOGGER.e(TAG, "A long click tap on LatLon cannot be handled, mMapObject is null!");
          break;
        }
        final double lat = mMapObject.getLat();
        final double lon = mMapObject.getLon();
        items.add(Framework.nativeFormatLatLon(lat, lon, false));
        items.add(Framework.nativeFormatLatLon(lat, lon, true));
        break;
      case R.id.ll__place_website:
        items.add(mTvWebsite.getText().toString());
        break;
      case R.id.ll__place_email:
        items.add(mTvEmail.getText().toString());
        break;
      case R.id.ll__place_phone:
        items.add(mTvPhone.getText().toString());
        break;
      case R.id.ll__place_schedule:
        items.add(mFullOpeningHours.getText().toString());
        break;
      case R.id.ll__place_operator:
        items.add(mTvOperator.getText().toString());
        break;
      case R.id.ll__place_wiki:
        if (mMapObject == null)
        {
          LOGGER.e(TAG, "A long click tap on wiki cannot be handled, mMapObject is null!");
          break;
        }
        items.add(mMapObject.getMetadata(Metadata.MetadataType.FMD_WIKIPEDIA));
        break;
    }

    final String copyText = getResources().getString(android.R.string.copy);
    for (int i = 0; i < items.size(); i++)
      menu.add(Menu.NONE, i, i, String.format("%s %s", copyText, items.get(i)));

    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
    {
      @Override
      public boolean onMenuItemClick(MenuItem item)
      {
        final int id = item.getItemId();
        final Context ctx = getContext();
        Utils.copyTextToClipboard(ctx, items.get(id));
        Utils.toastShortcut(ctx, ctx.getString(R.string.copied_to_clipboard, items.get(id)));
        Statistics.INSTANCE.trackEvent(Statistics.EventName.PP_METADATA_COPY + ":" + tagStr);
        AlohaHelper.logClick(AlohaHelper.PP_METADATA_COPY + ":" + tagStr);
        return true;
      }
    });

    popup.show();
    return true;
  }

  int getDockedWidth()
  {
    int res = getWidth();
    return (res == 0 ? getLayoutParams().width : res);
  }

  MwmActivity.LeftAnimationTrackListener getLeftAnimationTrackListener()
  {
    return mLeftAnimationTrackListener;
  }

  public void setLeftAnimationTrackListener(MwmActivity.LeftAnimationTrackListener listener)
  {
    mLeftAnimationTrackListener = listener;
  }

  public void hide()
  {
    detachCountry();
    setState(State.HIDDEN);
  }

  public boolean isHidden()
  {
    return (getState() == State.HIDDEN);
  }

  @SuppressWarnings("SimplifiableIfStatement")
  public boolean hideOnTouch()
  {
    if (mIsDocked || mIsFloating)
      return false;

    if (getState() == State.DETAILS || getState() == State.FULLSCREEN)
    {
      hide();
      return true;
    }

    return false;
  }

  private static boolean isInvalidDownloaderStatus(int status)
  {
    return (status != CountryItem.STATUS_DOWNLOADABLE &&
            status != CountryItem.STATUS_ENQUEUED &&
            status != CountryItem.STATUS_FAILED &&
            status != CountryItem.STATUS_PARTLY &&
            status != CountryItem.STATUS_PROGRESS);
  }

  private void updateDownloader(CountryItem country)
  {
    if (isInvalidDownloaderStatus(country.status))
    {
      if (mStorageCallbackSlot != 0)
        UiThread.runLater(mDownloaderDeferredDetachProc);
      return;
    }

    mDownloaderIcon.update(country);

    StringBuilder sb = new StringBuilder(StringUtils.getFileSizeString(country.totalSize));
    if (country.isExpandable())
      sb.append(String.format(Locale.US, "  •  %s: %d", getContext().getString(R.string.downloader_status_maps), country.totalChildCount));

    mDownloaderInfo.setText(sb.toString());
  }

  private void updateDownloader()
  {
    if (mCurrentCountry == null)
      return;

    mCurrentCountry.update();
    updateDownloader(mCurrentCountry);
  }

  private void attachCountry(String country)
  {
    CountryItem map = CountryItem.fill(country);
    if (isInvalidDownloaderStatus(map.status))
      return;

    mCurrentCountry = map;
    if (mStorageCallbackSlot == 0)
      mStorageCallbackSlot = MapManager.nativeSubscribe(mStorageCallback);

    mDownloaderIcon.show(true);
    UiUtils.show(mDownloaderInfo);
    updateDownloader(mCurrentCountry);
  }

  private void detachCountry()
  {
    if (mStorageCallbackSlot == 0)
      return;

    MapManager.nativeUnsubscribe(mStorageCallbackSlot);
    mStorageCallbackSlot = 0;
    mCurrentCountry = null;
    mDownloaderIcon.show(false);
    UiUtils.hide(mDownloaderInfo);
  }

  MwmActivity getActivity()
  {
    return (MwmActivity) getContext();
  }

  @Override
  public void onBookmarkSaved(int categoryId, int bookmarkId)
  {
    setMapObject(BookmarkManager.INSTANCE.getBookmark(categoryId, bookmarkId), true, null);
  }

  public boolean isBannerTouched(@NonNull MotionEvent event)
  {
    return mBannerController != null && mBannerController.isActionButtonTouched(event);
  }

  public boolean isLeaveReviewButtonTouched(@NonNull MotionEvent event)
  {
    return mUgcController != null && mUgcController.isLeaveReviewButtonTouched(event);
  }

  @Override
  public void onSizeChanged()
  {
    if (mBannerController != null && mBannerController.hasErrorOccurred())
    {
      mPreview.setPadding(mPreview.getPaddingLeft(), mPreview.getPaddingTop(),
                          getPaddingRight(), mMarginBase);
    }
  }
}
