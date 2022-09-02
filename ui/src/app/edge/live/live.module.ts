import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SharedModule } from './../../shared/shared.module';
import { Common_Autarchy } from './common/autarchy/Common_Autarchy';
import { Common_Consumption } from './common/consumption/Common_Consumption';
import { Common_Grid } from './common/grid/Common_Grid';
import { Common_Production } from './common/production/Common_Production';
import { Common_Selfconsumption } from './common/selfconsumption/Common_Selfconsumption';
import { StorageModalComponent } from './common/storage/modal/modal.component';
import { StorageComponent } from './common/storage/storage.component';
import { Controller_ChannelthresholdComponent } from './Controller/Channelthreshold/Channelthreshold';
import { Controller_ChpSocComponent } from './Controller/ChpSoc/ChpSoc';
import { Controller_ChpSocModalComponent } from './Controller/ChpSoc/modal/modal.component';
import { Controller_Ess_FixActivePower } from './Controller/Ess/FixActivePower/Ess_FixActivePower';
import { Controller_Ess_GridOptimizedCharge } from './Controller/Ess/GridOptimizedCharge/Ess_GridOptimizedCharge';
import { PredictionChartComponent } from './Controller/Ess/GridOptimizedCharge/modal/predictionChart';
import { Controller_Ess_TimeOfUseTariff_Discharge } from './Controller/Ess/Time-Of-Use-Tariff_Discharge/Ess_Time-Of-Use-Tariff_Discharge';
import { Controller_Ess_TimeOfUseTariff_DischargeModalComponent } from './Controller/Ess/Time-Of-Use-Tariff_Discharge/modal/modal.component';
import { Controller_EvcsComponent } from './Controller/Evcs/Evcs';
import { AdministrationComponent } from './Controller/Evcs/modal/administration/administration.component';
import { Controller_EvcsModalComponent } from './Controller/Evcs/modal/modal.page';
import { Controller_EvcsPopoverComponent } from './Controller/Evcs/modal/popover/popover.page';
import { Controller_Io_ChannelSingleThresholdComponent } from './Controller/Io/ChannelSingleThreshold/Io_ChannelSingleThreshold';
import { Controller_Io_ChannelSingleThresholdModalComponent } from './Controller/Io/ChannelSingleThreshold/modal/modal.component';
import { Controller_Io_FixDigitalOutput } from './Controller/Io/FixDigitalOutput/Io_FixDigitalOutput';
import { Controller_Io_FixDigitalOutputModalComponent } from './Controller/Io/FixDigitalOutput/modal/modal.component';
import { Controller_Io_HeatingElementComponent } from './Controller/Io/HeatingElement/Io_HeatingElement';
import { Controller_Io_HeatingElementModalComponent } from './Controller/Io/HeatingElement/modal/modal.component';
import { Controller_Io_HeatpumpComponent } from './Controller/Io/Heatpump/Io_Heatpump';
import { Controller_Io_HeatpumpModalComponent } from './Controller/Io/Heatpump/modal/modal.component';
import { Controller_Asymmetric_PeakShavingComponent } from './Controller/PeakShaving/Asymmetric/Asymmetric';
import { Controller_Asymmetric_PeakShavingModalComponent } from './Controller/PeakShaving/Asymmetric/modal/modal.component';
import { Controller_Symmetric_PeakShavingModalComponent } from './Controller/PeakShaving/Symmetric/modal/modal.component';
import { Controller_Symmetric_PeakShavingComponent } from './Controller/PeakShaving/Symmetric/Symmetric';
import { Controller_Symmetric_TimeSlot_PeakShavingModalComponent } from './Controller/PeakShaving/Symmetric_TimeSlot/modal/modal.component';
import { Controller_Symmetric_TimeSlot_PeakShavingComponent } from './Controller/PeakShaving/Symmetric_TimeSlot/Symmetric_TimeSlot';
import { DelayedSellToGridComponent } from './delayedselltogrid/delayedselltogrid.component';
import { DelayedSellToGridModalComponent } from './delayedselltogrid/modal/modal.component';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { InfoComponent } from './info/info.component';
import { Io_Api_DigitalInputComponent } from './Io/Api_DigitalInput/Io_Api_DigitalInput';
import { Io_Api_DigitalInput_ModalComponent } from './Io/Api_DigitalInput/modal/modal.component';
import { LiveComponent } from './live.component';
import { Evcs_Api_Cluster } from './Multiple/Evcs_Api_Cluster/Evcs_Api_Cluster';
import { EvcsChart } from './Multiple/Evcs_Api_Cluster/modal/evcs-chart/evcs.chart';
import { Evcs_Api_ClusterModalComponent } from './Multiple/Evcs_Api_Cluster/modal/evcsCluster-modal.page';
import { OfflineComponent } from './offline/offline.component';

@NgModule({
  imports: [
    BrowserAnimationsModule,
    BrowserModule,
    // Common
    Common_Autarchy,
    Common_Production,
    Common_Selfconsumption,
    Common_Consumption,
    Common_Grid,
    // Controller
    Controller_Ess_FixActivePower,
    Controller_Ess_GridOptimizedCharge,
    EnergymonitorModule,
    SharedModule,
  ],
  entryComponents: [
    AdministrationComponent,
    Controller_Asymmetric_PeakShavingModalComponent,
    Controller_ChpSocComponent,
    Controller_ChpSocModalComponent,
    Controller_Ess_TimeOfUseTariff_Discharge,
    Controller_Ess_TimeOfUseTariff_DischargeModalComponent,
    Controller_EvcsModalComponent,
    Controller_EvcsPopoverComponent,
    Controller_Io_ChannelSingleThresholdComponent,
    Controller_Io_ChannelSingleThresholdModalComponent,
    Controller_Io_FixDigitalOutput,
    Controller_Io_FixDigitalOutputModalComponent,
    Controller_Io_HeatingElementModalComponent,
    Controller_Io_HeatpumpModalComponent,
    Controller_Symmetric_PeakShavingComponent,
    Controller_Symmetric_TimeSlot_PeakShavingModalComponent,
    DelayedSellToGridModalComponent,
    Evcs_Api_ClusterModalComponent,
    Io_Api_DigitalInput_ModalComponent,
    Io_Api_DigitalInputComponent,
    StorageModalComponent,
  ],
  declarations: [
    AdministrationComponent,
    Controller_Asymmetric_PeakShavingComponent,
    Controller_Asymmetric_PeakShavingModalComponent,
    Controller_ChannelthresholdComponent,
    Controller_ChpSocComponent,
    Controller_ChpSocComponent,
    Controller_ChpSocModalComponent,
    Controller_Ess_TimeOfUseTariff_Discharge,
    Controller_Ess_TimeOfUseTariff_DischargeModalComponent,
    Controller_EvcsComponent,
    Controller_EvcsModalComponent,
    Controller_EvcsPopoverComponent,
    Controller_Io_ChannelSingleThresholdComponent,
    Controller_Io_ChannelSingleThresholdModalComponent,
    Controller_Io_FixDigitalOutput,
    Controller_Io_FixDigitalOutputModalComponent,
    Controller_Io_HeatingElementComponent,
    Controller_Io_HeatingElementModalComponent,
    Controller_Io_HeatpumpComponent,
    Controller_Io_HeatpumpModalComponent,
    Controller_Symmetric_PeakShavingComponent,
    Controller_Symmetric_PeakShavingModalComponent,
    Controller_Symmetric_TimeSlot_PeakShavingComponent,
    Controller_Symmetric_TimeSlot_PeakShavingModalComponent,
    DelayedSellToGridComponent,
    DelayedSellToGridModalComponent,
    Evcs_Api_Cluster,
    Evcs_Api_ClusterModalComponent,
    EvcsChart,
    InfoComponent,
    Io_Api_DigitalInput_ModalComponent,
    Io_Api_DigitalInputComponent,
    LiveComponent,
    OfflineComponent,
    StorageComponent,
    StorageModalComponent,
  ]
})
export class LiveModule { }
