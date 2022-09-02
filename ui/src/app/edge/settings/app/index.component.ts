import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { timeout } from 'rxjs/operators';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { environment } from 'src/environments';
import { Service, Websocket } from '../../../shared/shared';
import { GetApps } from './jsonrpc/getApps';

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
})
export class IndexComponent {

  private static readonly SELECTOR = "appIndex";
  public readonly spinnerId: string = IndexComponent.SELECTOR;

  /**
   * e. g. if more than 4 apps are in a list the apps are displayed in their categories
   */
  private static readonly MAX_APPS_IN_LIST = 4;

  public apps: GetApps.App[] = [];

  public installedApps: AppList = { name: this.translate.instant('Edge.Config.App.installed'), appCategories: [] };
  public availableApps: AppList = { name: this.translate.instant('Edge.Config.App.available'), appCategories: [] };
  // TODO incompatible apps should not be shown in the future
  public incompatibleApps: AppList = { name: this.translate.instant('Edge.Config.App.incompatible'), appCategories: [] };

  public appLists: AppList[] = [this.installedApps, this.availableApps, this.incompatibleApps];

  public categories = [];

  public constructor(
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
    private translate: TranslateService,
  ) {
  }

  private ionViewWillEnter() {
    // gets always called when entering the page
    this.init()
  }

  private init() {
    this.service.startSpinner(this.spinnerId);

    this.appLists.forEach(element => {
      element.appCategories = []
    });

    this.service.setCurrentComponent(environment.edgeShortName + " Apps", this.route).then(edge => {
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GetApps.Request()
        })).then(response => {

          this.service.stopSpinner(this.spinnerId);

          this.apps = (response as GetApps.Response).result.apps;

          // init categories
          this.apps.forEach(a => {
            a.categorys.forEach(category => {
              if (!this.categories.find(c => c.val.name === category.name)) {
                this.categories.push({ val: category, isChecked: true })
              }
            });
          })

          this.updateSelection(null)

        }).catch(reason => {
          console.error(reason.error);
          this.service.toast("Error while receiving available apps: " + reason.error.message, 'danger');
        });
    });
  }

  /**
   * Updates the slected categories.
   * @param event the event of a click on a 'ion-fab-list' to stop it from closing
   */
  private updateSelection(event) {
    if (event != null) {
      event.stopPropagation()
    }
    this.installedApps.appCategories = []
    this.availableApps.appCategories = []

    var sortedApps = []
    this.apps.forEach(a => {
      a.categorys.every(category => {
        var cat = this.categories.find(c => c.val.name === category.name)
        if (cat.isChecked) {
          sortedApps.push(a)
          return false
        }
        return true
      })
    })

    sortedApps.forEach(a => {
      if (a.instanceIds.length > 0) {
        this.pushIntoCategorie(a, this.installedApps)
        if (a.cardinality === 'MULTIPLE' && a.status.name !== 'INCOMPATIBLE') {
          this.pushIntoCategorie(a, this.availableApps)
        }
      } else {
        if (a.status.name === 'INCOMPATIBLE') {
          this.pushIntoCategorie(a, this.incompatibleApps)
        } else {
          this.pushIntoCategorie(a, this.availableApps)
        }
      }
    })
  }

  private pushIntoCategorie(app: GetApps.App, list: AppList) {
    app.categorys.forEach(category => {
      var catList = list.appCategories.find(l => l.category.name === category.name)
      if (catList == undefined) {
        catList = { category: category, apps: [] }
        list.appCategories.push(catList)
      }
      catList.apps.push(app)
    })
  }

  private showCategories(app: AppList) {
    return this.sum(app) > IndexComponent.MAX_APPS_IN_LIST
  }

  private isEmpty(app: AppList) {
    return this.sum(app) === 0
  }

  private sum(app: AppList) {
    let sum = 0
    app.appCategories.forEach(element => {
      sum += element.apps.length
    });
    return sum
  }

}

interface AppList {
  name: string,
  appCategories: AppListByCategorie[];
}

interface AppListByCategorie {
  category: GetApps.Category,
  apps: GetApps.App[];
}
