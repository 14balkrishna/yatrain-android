package com.yatrain.app.i18n

import androidx.compose.runtime.compositionLocalOf

data class AppStrings(
    // ── Language code ─────────────────────────────────────────────────────
    val langCode: String = "en",

    // ── Navigation tabs ───────────────────────────────────────────────────
    val tabStation: String,
    val tabTrack: String,
    val tabSleep: String,
    val tabYou: String,

    // ── Common ────────────────────────────────────────────────────────────
    val findTrain: String,
    val searchHint: String,
    val viewAll: String,
    val cancel: String,

    // ── Station / Home hero ───────────────────────────────────────────────
    val homeEyebrow: String,
    val homeTitle: String,
    val homeAccent: String,

    // ── Station home sections ─────────────────────────────────────────────
    val popularRightNow: String,
    val yourTicket: String,
    val yourTicketSub: String,
    val coachMap: String,
    val findYourCoach: String,

    // ── Search screen ─────────────────────────────────────────────────────
    val noTrainsFound: String,
    val partnerOffers: String,
    val ads: String,
    val quickServices: String,
    val enterPnrToFindCoach: String,
    val coachAssignmentNotFound: String,
    val enterPnrAbove: String,
    val coachesCount: String,
    val noTrainsFoundShort: String,
    val enableLocationToSeeTrains: String,

    // ── Coach map card ────────────────────────────────────────────────────
    val directionTowards: String,
    val directionDefault: String,

    // ── PNR card ──────────────────────────────────────────────────────────
    val pnrStatus: String,
    val departure: String,
    val passengers: String,
    val classLabel: String,
    val seatLabel: String,
    val chartLabel: String,
    val pnrLabel: String,
    val trainLabel: String,
    val statusLabel: String,
    val quotaLabel: String,
    val fareLabel: String,
    val hideTicket: String,
    val trackArrow: String,
    val viewRouteSetAlerts: String,
    val saveTicket: String,
    val ticketSaved: String,
    val fromLabel: String,
    val toLabel: String,

    // ── Track hero ────────────────────────────────────────────────────────
    val trackEyebrow: String,
    val trackTitle: String,
    val trackAccent: String,

    // ── Track screen ──────────────────────────────────────────────────────
    val liveTrainsToday: String,
    val showingDefaultTimes: String,
    val liveTrainsBtn: String,
    val colTrain: String,
    val colRoute: String,
    val colDep: String,
    val statusOnTime: String,
    val detectingStation: String,
    val locatingStation: String,
    val enterTrainNumberHint: String,

    // ── Track detail ──────────────────────────────────────────────────────
    val journeyTimeLeft: String,
    val nextStop: String,
    val speedLabel: String,
    val progressLabel: String,
    val statusLabelShort: String,
    val openLiveMap: String,
    val saveTrain: String,
    val savedLabel: String,
    val nowAt: String,
    val clearLabel: String,
    val theStream: String,
    val stationsAlongRoute: String,
    val tapToSetAlert: String,
    val trainIsHereNow: String,
    val alertLabel: String,
    val coachPosition: String,
    val searchByPnrToSeeCoach: String,
    val nowAtStation: String,
    val stationMap: String,
    val fileComplaint: String,
    val liveLabel: String,
    val trainPrefix: String,
    val youLabel: String,
    val coachLabel: String,
    val berthLabel: String,
    val routeLabel: String,
    val stopsLabel: String,
    val tapSetAlertPre: String,
    val tapSetAlertPost: String,
    val hoursUnit: String,

    // ── Alert picker ──────────────────────────────────────────────────────
    val arrivalAlert: String,
    val wakeMeBeforeArrival: String,
    val setAlertMin: String,
    val minLabel: String,

    // ── Station map sheet ─────────────────────────────────────────────────
    val platformNavigation: String,
    val imOnLabel: String,
    val takeMeToLabel: String,
    val platformLabel: String,
    val closeLabel: String,
    val stationSchematic: String,
    val onThisStation: String,
    val walkMinutes: String,
    val samePlatformWalk: String,
    val useFootOverBridge: String,

    // ── Sleep / Alerts hero ───────────────────────────────────────────────
    val sleepEyebrow: String,
    val sleepTitle: String,
    val sleepAccent: String,

    // ── Sleep screen ──────────────────────────────────────────────────────
    val smartSleep: String,
    val smartSleepDesc: String,
    val savedTrainsSection: String,
    val yourWatchlist: String,
    val noSavedTrainsTitle: String,
    val noSavedTrainsMsg: String,
    val alertsSection: String,
    val activeAlerts: String,
    val noAlertsTitle: String,
    val noAlertsMsg: String,
    val activeAlertsLabel: String,
    val stationAlertsTitle: String,
    val savedTicketsLabel: String,
    val yourBookingsLabel: String,
    val noSavedTicketsTitle: String,
    val noSavedTicketsMsg: String,

    // ── Profile hero ──────────────────────────────────────────────────────
    val profileEyebrow: String,
    val profileTitle: String,

    // ── Profile screen ────────────────────────────────────────────────────
    val yatrainMember: String,
    val pro: String,
    val free: String,
    val passenger: String,
    val trips: String,
    val savedStat: String,
    val alertsStat: String,
    val yatrainPro: String,
    val upgradeToPro: String,
    val upgradeDesc: String,
    val travel: String,
    val savedTrainsMenu: String,
    val searchHistory: String,
    val notificationPrefs: String,
    val about: String,
    val privacyPolicy: String,
    val termsOfUse: String,
    val support: String,
    val contact: String,
    val signOut: String,
    val signOutConfirm: String,

    // ── Auth screen ───────────────────────────────────────────────────────
    val authTagline: String,
    val authSignIn: String,
    val authCreateAccount: String,
    val authResetPassword: String,
    val authFullName: String,
    val authEmail: String,
    val authPassword: String,
    val authSendResetLink: String,
    val authForgotPassword: String,
    val authBackToSignIn: String,

    // ── Language picker ───────────────────────────────────────────────────
    val selectLanguage: String,

    // ── Pro / Subscription ────────────────────────────────────────────────
    val proMonthlyPrice: String,
    val proAnnualPrice: String,
    val proMonthly: String,
    val proAnnual: String,
    val bestValue: String,
    val manageSubscription: String,
    val proActiveMember: String,
    val signInToUnlock: String,
    val restorePurchases: String,
    val proTagline: String,
    val freeColumnHeader: String,
    val proColumnHeader: String,

    // ── Feature gating ────────────────────────────────────────────────────
    val proLockedFeature: String,
    val freePnrHint: String,
    val alertLimitReached: String,
    val alertLimitMsg: String,
    val proSaveTicket: String,
    val proSaveTrain: String,
    val proCoachFinder: String,
)

/** CompositionLocal that provides the active [AppStrings] to the tree. */
val LocalStrings = compositionLocalOf<AppStrings> { EnglishStrings }
