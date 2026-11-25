package com.marketiq.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MarketiQScreen(val label: String) {
    Welcome("Welcome"),
    Dashboard("Dashboard"),
    Campaigns("Campaigns"),
    Reports("Reports")
}

enum class MarketingChannel(val displayName: String) {
    GoogleAds("Google Ads"),
    Instagram("Instagram"),
    LinkedIn("LinkedIn"),
    Email("Email")
}

enum class CampaignStatus {
    Active,
    Scheduled,
    Paused
}

data class Campaign(
    val id: String,
    val name: String,
    val channel: MarketingChannel,
    val budget: Int,
    val leads: Int,
    val conversionRate: Double,
    val status: CampaignStatus
)

data class ChannelSummary(
    val channel: MarketingChannel,
    val campaignCount: Int,
    val totalBudget: Int,
    val totalLeads: Int,
    val costPerLead: Double
)

data class CampaignReport(
    val campaign: Campaign,
    val costPerLead: Double,
    val roiMultiple: Double,
    val roiRating: String
)

data class MarketiQUiState(
    val currentScreen: MarketiQScreen = MarketiQScreen.Welcome,
    val marketerEmail: String = "alex.marketing@company.com",
    val campaigns: List<Campaign> = initialSampleCampaigns()
) {
    val totalAdSpend: Int
        get() = campaigns.sumOf { it.budget }

    val activeLeads: Int
        get() = campaigns.sumOf { it.leads }

    val avgConversionRate: Double
        get() = if (campaigns.isEmpty()) 0.0
        else ((campaigns.sumOf { it.conversionRate } / campaigns.size) * 10.0).roundToInt() / 10.0

    val channelSummaries: List<ChannelSummary>
        get() = MarketingChannel.entries.map { ch ->
            val matching = campaigns.filter { it.channel == ch }
            val spend = matching.sumOf { it.budget }
            val leads = matching.sumOf { it.leads }
            ChannelSummary(
                channel = ch,
                campaignCount = matching.size,
                totalBudget = spend,
                totalLeads = leads,
                costPerLead = if (leads > 0) spend.toDouble() / leads else 0.0
            )
        }

    val campaignReports: List<CampaignReport>
        get() = campaigns.map { campaign ->
            val cpl = if (campaign.leads > 0) campaign.budget.toDouble() / campaign.leads else 0.0
            val leadValue = when (campaign.channel) {
                MarketingChannel.LinkedIn -> 142.0
                MarketingChannel.GoogleAds -> 124.0
                MarketingChannel.Email -> 112.0
                MarketingChannel.Instagram -> 91.0
            }
            val roi = if (campaign.budget > 0) (campaign.leads * leadValue) / campaign.budget else 0.0
            val roundedRoi = (roi * 10.0).roundToInt() / 10.0
            val rating = when {
                roundedRoi >= 4.0 -> "High ROI"
                roundedRoi >= 3.0 -> "Strong ROI"
                roundedRoi >= 1.8 -> "Moderate ROI"
                else -> "Needs Review"
            }
            CampaignReport(
                campaign = campaign,
                costPerLead = cpl,
                roiMultiple = roundedRoi,
                roiRating = rating
            )
        }
}

private fun initialSampleCampaigns(): List<Campaign> = listOf(
    Campaign("1", "Q4 LinkedIn B2B Outreach", MarketingChannel.LinkedIn, 4800, 148, 6.4, CampaignStatus.Active),
    Campaign("2", "Google Search Retargeting", MarketingChannel.GoogleAds, 4500, 124, 4.9, CampaignStatus.Active),
    Campaign("3", "Instagram Festive Promo", MarketingChannel.Instagram, 3800, 88, 3.5, CampaignStatus.Active),
    Campaign("4", "Newsletter Drip", MarketingChannel.Email, 1400, 60, 4.4, CampaignStatus.Scheduled)
)

class MarketiQViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MarketiQUiState())
    val uiState: StateFlow<MarketiQUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.update { it.copy(marketerEmail = email) }
    }

    fun navigateTo(screen: MarketiQScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun launchWorkspace() {
        navigateTo(MarketiQScreen.Dashboard)
    }

    fun addCampaign(name: String, channel: MarketingChannel, budget: Int, leads: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || budget <= 0 || leads < 0) return

        val baseCvr = when (channel) {
            MarketingChannel.LinkedIn -> 5.8
            MarketingChannel.GoogleAds -> 4.8
            MarketingChannel.Email -> 4.5
            MarketingChannel.Instagram -> 3.6
        }
        val efficiency = if (budget > 0 && leads > 0) (leads.toDouble() / budget) * 35.0 else 1.0
        val derivedCvr = ((min(9.5, max(2.1, baseCvr * (0.75 + 0.25 * efficiency)))) * 10.0).roundToInt() / 10.0

        val newCampaign = Campaign(
            id = System.currentTimeMillis().toString(),
            name = trimmed,
            channel = channel,
            budget = budget,
            leads = leads,
            conversionRate = derivedCvr,
            status = CampaignStatus.Active
        )

        _uiState.update { state ->
            state.copy(campaigns = listOf(newCampaign) + state.campaigns)
        }
    }
}
