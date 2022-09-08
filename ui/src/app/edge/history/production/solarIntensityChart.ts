import {formatNumber} from '@angular/common';
import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {TranslateService} from '@ngx-translate/core';
import {DefaultTypes} from 'src/app/shared/service/defaulttypes';
import {ChannelAddress, Edge, EdgeConfig, Service} from '../../../shared/shared';
import {AbstractHistoryChart} from '../abstracthistorychart';
import {Data, TooltipItem} from '../shared';

@Component({
  selector: 'solarIntensityChart',
  templateUrl: '../abstracthistorychart.html'
})
export class SolarIntensityChart extends AbstractHistoryChart implements OnInit, OnChanges {

  @Input() public period: DefaultTypes.HistoryPeriod;

  ngOnChanges() {
    this.updateChart();
  }

  constructor(
    protected service: Service,
    protected translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super("solar-intensity-chart", service, translate);
  }

  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent('', this.route);
  }

  ngOnDestroy() {
    this.unsubscribeChartRefresh()
  }

  protected updateChart() {
    this.autoSubscribeChartRefresh();
    this.startSpinner();
    this.loading = true;
    this.colors = [];
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      this.service.getCurrentEdge().then(edge => {
        this.service.getConfig().then(config => {
          let result = response.result;
          // convert labels
          let labels: Date[] = [];
          for (let timestamp of result.timestamps) {
            labels.push(new Date(timestamp));
          }
          this.labels = labels;

          // convert datasets
          let datasets = [];
          this.getChannelAddresses(edge, config).then(channelAddresses => {
            channelAddresses.forEach(channelAddress => {

              if (!result.data[channelAddress.toString()]) {
                return
              }

              let data = result.data[channelAddress.toString()];

              if (!data) {
                return;
              } else {
                if (channelAddress.channelId == "SunIntensity") {
                  datasets.push({
                    label: this.translate.instant('General.sunIntensity'),
                    data: data
                  });
                  this.colors.push({
                    backgroundColor: 'rgba(253,197,7,0.05)',
                    borderColor: 'rgba(253,197,7,1)',
                  });
                }
              }
            });
          });
          this.datasets = datasets;
          this.loading = false;
          this.stopSpinner();

        }).catch(reason => {
          console.error(reason); // TODO error message
          this.initializeChart();
          return;
        });

      }).catch(reason => {
        console.error(reason); // TODO error message
        this.initializeChart();
        return;
      });

    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    });
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {

    return new Promise((resolve) => {
      let result: ChannelAddress[] = [
        new ChannelAddress('_weather', 'SunIntensity')
      ];
      resolve(result);
    })
  }

  protected setLabel() {
    let options = this.createDefaultChartOptions();
    options.scales.yAxes[0].scaleLabel.labelString = "W/m2";
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      return label + ": " + formatNumber(value, 'de', '1.0-2') + " W/m2a";
    }
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 21 * 9;
  }
}
