package uk.gov.justice.laa.dstew.claimsreports.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Getter;

/**
 * The `Report000Entity` class provides a representation of the various fields in the `mvw_report_000` materialised view.
 */
@Entity
@Getter
@Table(name = "mvw_report_000", schema = "claims")
public class Report000Entity implements Serializable {

  @Id
  @Column(name = "claim_id")
  private String claimId;

  @Column(name = "office_account_number")
  private String officeAccountNumber;

  @Column(name = "submission_period")
  private String submissionPeriod;

  @Column(name = "submission_for_date")
  private String submissionForDate;

  @Column(name = "date_submitted")
  private String dateSubmitted;

  @Column(name = "submission_status")
  private String submissionStatus;

  @Column(name = "area_of_law")
  private String areaOfLaw;

  @Column(name = "crime_submission_reference")
  private String crimeSubmissionReference;

  @Column(name = "civil_submission_reference")
  private String civilSubmissionReference;

  @Column(name = "mediation_submission_reference")
  private String mediationSubmissionReference;

  @Column(name = "line_number")
  private String lineNumber;

  @Column(name = "case_reference_number")
  private String caseReferenceNumber;

  @Column(name = "unique_file_number")
  private String uniqueFileNumber;

  @Column(name = "case_id")
  private String caseId;

  @Column(name = "case_start_date")
  private String caseStartDate;

  @Column(name = "case_concluded_date")
  private String caseConcludedDate;

  @Column(name = "matter_type_code")
  private String matterTypeCode;

  @Column(name = "matter_type_1")
  private String matterType1;

  @Column(name = "matter_type_2")
  private String matterType2;

  @Column(name = "case_stage_level")
  private String caseStageLevel;

  @Column(name = "stage_reached")
  private String stageReached;

  @Column(name = "outcome_code")
  private String outcomeCode;

  @Column(name = "transfer_date")
  private String transferDate;

  @Column(name = "client_forename")
  private String clientForename;

  @Column(name = "client_surname")
  private String clientSurname;

  @Column(name = "client_date_of_birth")
  private String clientDateOfBirth;

  @Column(name = "unique_client_number")
  private String uniqueClientNumber;

  @Column(name = "home_office_client_number")
  private String homeOfficeClientNumber;

  @Column(name = "gender")
  private String gender;

  @Column(name = "ethnicity")
  private String ethnicity;

  @Column(name = "disability")
  private String disability;

  @Column(name = "client_postcode")
  private String clientPostcode;

  @Column(name = "client_type_code")
  private String clientTypeCode;

  @Column(name = "first_client_legally_aided")
  private String firstClientLegallyAided;

  @Column(name = "second_client_forename")
  private String secondClientForename;

  @Column(name = "second_client_surname")
  private String secondClientSurname;

  @Column(name = "second_client_date_of_birth")
  private String secondClientDateOfBirth;

  @Column(name = "second_client_postcode")
  private String secondClientPostcode;

  @Column(name = "second_client_gender")
  private String secondClientGender;

  @Column(name = "second_client_ethnicity")
  private String secondClientEthnicity;

  @Column(name = "second_client_disability")
  private String secondClientDisability;

  @Column(name = "second_client_legally_aided")
  private String secondClientLegallyAided;

  @Column(name = "category_of_law_code")
  private String categoryOfLawCode;

  @Column(name = "fee_code")
  private String feeCode;

  @Column(name = "fee_code_description")
  private String feeCodeDescription;

  @Column(name = "fee_type")
  private String feeType;

  @Column(name = "total_current_claim_value")
  private String totalCurrentClaimValue;

  @Column(name = "vat_rate_applied")
  private String vatRateApplied;

  @Column(name = "vat_indicator")
  private String vatIndicator;

  @Column(name = "waiting_time")
  private String waitingTime;

  @Column(name = "travel_time")
  private String travelTime;

  @Column(name = "advice_time")
  private String adviceTime;

  @Column(name = "profit_costs")
  private String profitCosts;

  @Column(name = "counsel_fees")
  private String counselFees;

  @Column(name = "disbursement_costs")
  private String disbursementCosts;

  @Column(name = "disbursement_vat_costs")
  private String disbursementVatCosts;

  @Column(name = "travel_waiting_costs")
  private String travelWaitingCosts;

  @Column(name = "jr_form_filling_costs")
  private String jrFormFillingCosts;

  @Column(name = "cost_/_damages_recovered")
  private String costDamagesRecovered;

  @Column(name = "detention_travel_&_waiting_costs")
  private String detentionTravelWaitingCosts;

  @Column(name = "adjourned_hearing_fee_count")
  private String adjournedHearingFeeCount;

  @Column(name = "mediation_sessions_count")
  private String mediationSessionsCount;

  @Column(name = "cmrh_oral_count")
  private String cmrhOralCount;

  @Column(name = "cmrh_telephone_count")
  private String cmrhTelephoneCount;

  @Column(name = "ho_interview_count")
  private String hoInterviewCount;

  @Column(name = "medical_reports_count")
  private String medicalReportsCount;

  @Column(name = "meetings_attended_code")
  private String meetingsAttendedCode;

  @Column(name = "current_vat_indicator")
  private String currentVatIndicator;

  @Column(name = "current_net_profit_costs_amount")
  private String currentNetProfitCostsAmount;

  @Column(name = "current_net_cost_of_counsel_amount")
  private String currentNetCostOfCounselAmount;

  @Column(name = "current_disbursement_amount")
  private String currentDisbursementAmount;

  @Column(name = "current_travel_and_waiting_costs_amount")
  private String currentTravelAndWaitingCostsAmount;

  @Column(name = "current_detention_and_waiting_costs_amount")
  private String currentDetentionAndWaitingCostsAmount;

  @Column(name = "current_jr_form_filling_amount")
  private String currentJrFormFillingAmount;

  @Column(name = "current_fixed_fee_amount")
  private String currentFixedFeeAmount;

  @Column(name = "current_escape_case_flag")
  private String currentEscapeCaseFlag;

  @Column(name = "current_hourly_total_amount")
  private String currentHourlyTotalAmount;

  @Column(name = "current_bolt_on_total_fee_amount")
  private String currentBoltOnTotalFeeAmount;

  @Column(name = "current_bolt_on_adjourned_hearing_count")
  private String currentBoltOnAdjournedHearingCount;

  @Column(name = "current_bolt_on_adjourned_hearing_fee")
  private String currentBoltOnAdjournedHearingFee;

  @Column(name = "current_bolt_on_cmrh_telephone_count")
  private String currentBoltOnCmrhTelephoneCount;

  @Column(name = "current_bolt_on_cmrh_telephone_fee")
  private String currentBoltOnCmrhTelephoneFee;

  @Column(name = "current_bolt_on_cmrh_oral_count")
  private String currentBoltOnCmrhOralCount;

  @Column(name = "current_bolt_on_cmrh_oral_fee")
  private String currentBoltOnCmrhOralFee;

  @Column(name = "current_bolt_on_home_office_interview_count")
  private String currentBoltOnHomeOfficeInterviewCount;

  @Column(name = "current_bolt_on_home_office_interview_fee")
  private String currentBoltOnHomeOfficeInterviewFee;

  @Column(name = "current_profit_costs_vat")
  private String currentProfitCostsVat;

  @Column(name = "current_counsel_costs_vat")
  private String currentCounselCostsVat;

  @Column(name = "current_travel_costs_vat")
  private String currentTravelCostsVat;

  @Column(name = "current_waiting_costs_vat")
  private String currentWaitingCostsVat;

  @Column(name = "current_jr_form_filling_costs_vat")
  private String currentJrFormFillingCostsVat;

  @Column(name = "current_bolt_on_fees_vat")
  private String currentBoltOnFeesVat;

  @Column(name = "legacy_case_flag")
  private String legacyCaseFlag;

  @Column(name = "london_rate_flag")
  private String londonRateFlag;

  @Column(name = "tolerance_indicator")
  private String toleranceIndicator;

  @Column(name = "substantive_hearing_flag")
  private String substantiveHearingFlag;

  @Column(name = "additional_travel_payment_flag")
  private String additionalTravelPaymentFlag;

  @Column(name = "local_authority_number")
  private String localAuthorityNumber;

  @Column(name = "procurement_area_code")
  private String procurementAreaCode;

  @Column(name = "access_point_code")
  private String accessPointCode;

  @Column(name = "referral_source_code")
  private String referralSourceCode;

  @Column(name = "ait_hearing_centre_code")
  private String aitHearingCentreCode;

  @Column(name = "ecf_reference")
  private String ecfReference;

  @Column(name = "exemption_criteria_satisfied")
  private String exemptionCriteriaSatisfied;

  @Column(name = "cla_reference_number")
  private String claReferenceNumber;

  @Column(name = "cla_exemption_code")
  private String claExemptionCode;

  @Column(name = "immigration_prior_authority_number")
  private String immigrationPriorAuthorityNumber;

  @Column(name = "postal_application_accepted")
  private String postalApplicationAccepted;

  @Column(name = "irc_surgery")
  private String ircSurgery;

  @Column(name = "surgery_date")
  private String surgeryDate;

  @Column(name = "number_of_clients_seen_at_the_surgery")
  private String surgeryClientsCount;

  @Column(name = "number_of_surgery_clients_resulting_in_a_legal_help_matter_opened")
  private String surgeryMattersCount;

  @Column(name = "nrm_advice")
  private String nrmAdvice;

  @Column(name = "prn_follow_on_work")
  private String prnFollowOnWork;

  // Getters and Setters omitted for brevity
}
