import { Component, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";

@Component({
    selector: 'oe-modal-info-line',
    templateUrl: './modal-info-line.html'
})
export class ModalInfoLine {

    /** Icon, displayed on the left side */
    @Input() icon: Icon;

    /**
     *  Info-Text, displayed on the right side, optional style for all lines
     *  Multiple lines with own style is possible
     *  */
    @Input() info: { text: string, lineStyle?: string }[] | string;

    @Input() lineStyle: string;

    @Input() rowStyle: string;
}