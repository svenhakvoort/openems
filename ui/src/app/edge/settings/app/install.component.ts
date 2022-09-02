import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { AddAppInstance } from './jsonrpc/addAppInstance';
import { GetAppAssistant } from './jsonrpc/getAppAssistant';

@Component({
  selector: InstallAppComponent.SELECTOR,
  templateUrl: './install.component.html'
})
export class InstallAppComponent implements OnInit {

  private static readonly SELECTOR = "appInstall";
  public readonly spinnerId: string = InstallAppComponent.SELECTOR;

  public form = null;
  public model = null;
  public fields: FormlyFieldConfig[] = null;

  private appId: string;
  private appName: string;
  private edge: Edge = null;
  private isInstalling: boolean;

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  public ngOnInit() {
    this.service.startSpinner(this.spinnerId);
    let appId = this.route.snapshot.params["appId"];
    this.appId = appId;
    this.service.setCurrentComponent("App " + appId, this.route).then(edge => {
      this.edge = edge
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GetAppAssistant.Request({ appId: appId })
        })).then(response => {

          let appAssistant = GetAppAssistant.postprocess((response as GetAppAssistant.Response).result);
          // insert alias field into appAssistent fields
          let aliasField = { key: "ALIAS", type: "input", templateOptions: { label: "Alias" }, defaultValue: appAssistant.alias };
          appAssistant.fields.splice(0, 0, aliasField)

          this.fields = appAssistant.fields;
          this.appName = appAssistant.name;
          this.model = {};
          this.form = new FormGroup({});
          this.service.stopSpinner(this.spinnerId);

        })
        .catch(reason => {
          console.error(reason.error);
          this.service.toast("Error while receiving App Assistant for [" + appId + "]: " + reason.error.message, 'danger');
        });
    });
  }

  public submit() {
    // remove alias field from properties
    let alias = this.form.value["ALIAS"]
    const clonedFields = {};
    for (let item in this.form.value) {
      if (item != "ALIAS") {
        clonedFields[item] = this.form.value[item]
      }
    }
    this.isInstalling = true
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: new AddAppInstance.Request({
          appId: this.appId,
          alias: alias,
          properties: clonedFields
        })
      })).then(response => {
        let result = (response as AddAppInstance.Response).result

        if (result.instance) {
          result.instanceId = result.instance.instanceId
          this.model = result.instance.properties
        }
        if (result.warnings && result.warnings.length > 0) {
          this.service.toast(result.warnings.join(";"), 'warning')
        } else {
          this.service.toast("Successfully installed App", 'success');
        }

        this.form.markAsPristine();
      }).catch(reason => {
        this.service.toast("Error installing App:" + reason.error.message, 'danger');
      }).finally(() => {
        this.isInstalling = false
      });
  }

}
