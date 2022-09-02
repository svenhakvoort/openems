import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
    templateUrl: './modal.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class Modal extends AbstractModal {

    public channelCapacity: number;
    public isAtLeastAdmin: boolean = false;
    public refreshChart: boolean;

    public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
    public readonly CONVERT_MINUTE_TO_TIME_OF_DAY = Utils.CONVERT_MINUTE_TO_TIME_OF_DAY;
    public readonly CONVERT_TO_WATTHOURS = Utils.CONVERT_TO_WATTHOURS;
    public readonly DelayChargeState = DelayChargeState;
    public state: string = '';
    public chargeLimit: { name: string, value: number };
    public delayChargeState: number = null;
    public maximumSellToGridPower: number = null;
    public targetMinute: number = null;
    public delayChargeMaximumChargeLimit: number = null;
    public targetEpochSeconds: number = null;
    public chargeStartEpochSeconds: number = null;

    protected override getChannelAddresses(): ChannelAddress[] {
        this.refreshChart = false;
        let channels: ChannelAddress[] = [];
        if (this.edge.roleIsAtLeast(Role.ADMIN)) {
            this.isAtLeastAdmin = true;
            if ('ess.id' in this.component.properties) {
                channels.push(
                    new ChannelAddress(this.component.properties['ess.id'], "Capacity"),
                )
            }
        }
        channels.push(
            new ChannelAddress(this.component.id, "SellToGridLimitState"),
            new ChannelAddress(this.component.id, "DelayChargeState"),
            new ChannelAddress(this.component.id, "SellToGridLimitMinimumChargeLimit"),
            new ChannelAddress(this.component.id, "_PropertyMaximumSellToGridPower"),
            new ChannelAddress(this.component.id, "_PropertySellToGridLimitEnabled"),
            new ChannelAddress(this.component.id, "TargetEpochSeconds"),
            new ChannelAddress(this.component.id, "PredictedChargeStartEpochSeconds")
        )
        return channels
    }

    protected override onCurrentData(currentData: CurrentData) {

        // If the gridfeed in Limit is avoided
        if (currentData.thisComponent['SellToGridLimitState'] == SellToGridLimitState.ACTIVE_LIMIT_FIXED ||
            (currentData.thisComponent['SellToGridLimitState'] == SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT &&
                currentData.thisComponent['DelayChargeState'] != DelayChargeState.ACTIVE_LIMIT &&
                currentData.thisComponent['SellToGridLimitMinimumChargeLimit'] > 0)) {
            this.chargeLimit = {
                name: this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.minimumCharge'),
                value: currentData.thisComponent['SellToGridLimitMinimumChargeLimit']
            }
            this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.gridFeedInLimitationIsAvoided');

        } else {

            // DelayCharge State
            switch (currentData.thisComponent['DelayChargeState']) {
                case -1: this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.notDefined');
                    break;
                case 0: this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.chargeLimitActive')
                    break;
                case 1: this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.passedEndTime')
                    break;
                case 2: this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.storageAlreadyFull')
                    break;
                case 3: this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.endTimeNotCalculated')
                    break;
                case 4: this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.noLimitPossible')
                    break;
                case 5:

                // Case 6: 'DISABLED' hides 'state-line', so no Message needed 
                case 7: this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.noLimitActive')
                    break;

                case 8: this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.chargingDelayed')
                    break;
            }

            // DelayCharge Maximum Charge Limit
            if (currentData.thisComponent['DelayChargeMaximumChargeLimit'] != null) {
                this.chargeLimit = {
                    name: this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.maximumCharge'),
                    value: currentData.thisComponent['DelayChargeMaximumChargeLimit']
                }
            }
        }
        this.delayChargeState = currentData.thisComponent['DelayChargeState'];

        // Capacity (visible for admin only)
        if (this.edge.roleIsAtLeast(Role.ADMIN) && 'ess.id' in this.component.properties) {
            this.channelCapacity = currentData.allComponents[this.component.properties['ess.id'] + '/Capacity']
        }

        this.maximumSellToGridPower = currentData.thisComponent['_PropertyMaximumSellToGridPower']
        this.targetMinute = currentData.thisComponent['TargetMinute'];
        this.delayChargeMaximumChargeLimit = currentData.thisComponent['DelayChargeMaximumChargeLimit'];
        this.targetEpochSeconds = currentData.thisComponent['TargetEpochSeconds'];
        this.chargeStartEpochSeconds = currentData.thisComponent['PredictedChargeStartEpochSeconds'];
    }

    protected override getFormGroup(): FormGroup {
        return this.formBuilder.group({
            mode: new FormControl(this.component.properties.mode),
            sellToGridLimitEnabled: new FormControl(this.component.properties.sellToGridLimitEnabled),
            maximumSellToGridPower: new FormControl(this.component.properties.maximumSellToGridPower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required
            ])),
            delayChargeRiskLevel: new FormControl(this.component.properties.delayChargeRiskLevel),
            manualTargetTime: new FormControl(this.component.properties.manualTargetTime)
        })
    }
}

export enum DelayChargeState {
    UNDEFINED = -1, // Undefined
    ACTIVE_LIMIT = 0, // Active limit
    NO_REMAINING_TIME = 1, // No remaining time
    NO_REMAINING_CAPACITY = 2, // No remaining capacity //
    TARGET_MINUTE_NOT_CALCULATED = 3, // Target minute not calculated //
    NO_FEASABLE_SOLUTION = 4, // Limit cannot be adapted because of other constraints with higher priority
    NO_CHARGE_LIMIT = 5, // No active limitation
    DISABLED = 6, // Delay charge part is disabled
    NOT_STARTED = 7, // Delay charge was not started because there is no production or to less production
    AVOID_LOW_CHARGING = 8, // Avoid charging with low power for more efficiency
}

export enum SellToGridLimitState {
    UNDEFINED = -1,// Undefined
    ACTIVE_LIMIT_FIXED = 0,// Active limitation - Fix limit
    NO_LIMIT = 1,//No active limitation
    NO_FEASABLE_SOLUTION = 2,//Limit cannot be adapted because of other constraints with higher priority
    ACTIVE_LIMIT_CONSTRAINT = 3,// Active limitation - Minimum charge power
    DISABLED = 4, // SellToGridLimit part is disabled
    NOT_STARTED = 5,//SellToGridLimit part was not started because there is no production or to less production
}