import { ChartOptionsPopoverComponent } from './popover/popover.component';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { Service } from '../shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'chartOptions',
    templateUrl: './chartoptions.component.html'
})
export class ChartOptionsComponent {

    @Input() public showPhases: boolean;
    @Input() public showTotal: boolean;
    @Input() public showSunIntensity: boolean;
    @Output() setShowPhases = new EventEmitter<boolean>();
    @Output() setShowTotal = new EventEmitter<boolean>();
    @Output() setShowSunIntensity = new EventEmitter<boolean>();

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverCtrl: PopoverController,
    ) { }

    async presentPopover(ev: any) {
        const popover = await this.popoverCtrl.create({
            component: ChartOptionsPopoverComponent,
            event: ev,
            translucent: false,
            componentProps: {
                showPhases: this.showPhases,
                showTotal: this.showTotal,
                showSunIntensity: this.showSunIntensity
            }
        });
        await popover.present();
        popover.onDidDismiss().then((data) => {
            if (data['role'] == "Phases" && data['data'] == true) {
                this.setShowPhases.emit(true);
            } else if (data['role'] == "Phases" && data['data'] == false) {
                this.setShowPhases.emit(false);
            }
            if (data['role'] == "Total" && data['data'] == true) {
                this.setShowTotal.emit(true);
            } else if (data['role'] == "Total" && data['data'] == false) {
                this.setShowTotal.emit(false);
            }

            if (data['role'] == "SunIntensity" && data['data'] == true) {
              this.setShowSunIntensity.emit(true);
            } else if (data['role'] == "SunIntensity" && data['data'] == false) {
              this.setShowSunIntensity.emit(false);
            }
        });
        await popover.present();
    }
}
