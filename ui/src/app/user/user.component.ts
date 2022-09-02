import { Component, OnInit } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../environments';
import { GetUserInformationRequest } from '../shared/jsonrpc/request/getUserInformationRequest';
import { SetUserInformationRequest } from '../shared/jsonrpc/request/setUserInformationRequest';
import { UpdateUserLanguageRequest } from '../shared/jsonrpc/request/updateUserLanguageRequest';
import { GetUserInformationResponse } from '../shared/jsonrpc/response/getUserInformationResponse';
import { Service, Websocket } from '../shared/shared';
import { Language, LanguageTag } from '../shared/translate/language';

export const COUNTRY_OPTIONS: { value: string; label: string }[] = [
  { value: 'de', label: 'Deutschland' },
  { value: 'at', label: 'Österreich' },
  { value: 'ch', label: 'Schweiz' },
];

type UserInformation = {
  firstname: string,
  lastname: string,
  email: string,
  phone: string,
  street: string,
  zip: string,
  city: string,
  country: string
}
@Component({
  templateUrl: './user.component.html'
})
export class UserComponent implements OnInit {

  public environment = environment;

  public readonly languages: LanguageTag[];
  public currentLanguage: LanguageTag;
  public isEditModeDisabled: boolean = true;
  public form: { formGroup: FormGroup, fields: FormlyFieldConfig[], model: UserInformation };
  public showInformation: boolean = false;

  constructor(
    public translate: TranslateService,
    public service: Service,
    private route: ActivatedRoute,
    private websocket: Websocket,
  ) {
    this.languages = Language.getLanguageTags();
  }

  ngOnInit() {
    // Set currentLanguage to 
    this.currentLanguage = LanguageTag[localStorage.LANGUAGE];
    this.service.setCurrentComponent(this.translate.instant('Menu.user'), this.route);

    this.getUserInformation().then((userInformation) => {
      this.form = {
        formGroup: new FormGroup({}),
        fields: this.getFields(),
        model: userInformation
      }
      this.showInformation = true;
    })
  }

  public applyChanges() {
    let user = {
      user: {
        lastname: this.form.model.lastname,
        firstname: this.form.model.firstname,
        email: this.form.model.email,
        phone: this.form.model.phone,
        address: {
          street: this.form.model.street,
          zip: this.form.model.zip,
          city: this.form.model.city,
          country: this.form.model.country
        },
      }
    }
    this.service.websocket.sendRequest(new SetUserInformationRequest(user)).then(() => {
      this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
    }).catch((reason) => {
      this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
    })
    this.enableAndDisableFormFields();
    this.form.formGroup.markAsPristine();
  }

  public enableAndDisableEditMode(): void {
    if (this.isEditModeDisabled == false) {
      this.getUserInformation().then((userInformation) => {
        this.form = {
          formGroup: new FormGroup({}),
          fields: this.getFields(),
          model: userInformation
        }
      });
    }

    this.enableAndDisableFormFields()
  }

  public enableAndDisableFormFields(): boolean {
    // Update Fields
    this.form?.fields[0].fieldGroup.forEach(element => {
      element.templateOptions.disabled = !element.templateOptions.disabled;
    });
    return this.isEditModeDisabled = !this.isEditModeDisabled;
  }


  public getFields(): FormlyFieldConfig[] {

    return [{
      fieldGroup: [
        {
          key: "firstname",
          type: "input",
          templateOptions: {
            label: this.translate.instant("Register.form.firstname"),
            disabled: true
          }
        },
        {
          key: "lastname",
          type: "input",
          templateOptions: {
            label: this.translate.instant("Register.form.lastname"),
            disabled: true
          }
        },
        {
          key: "street",
          type: "input",
          templateOptions: {
            label: this.translate.instant("Register.form.street"),
            disabled: true
          }
        },
        {
          key: "zip",
          type: "input",
          templateOptions: {
            label: this.translate.instant("Register.form.zip"),
            disabled: true
          }
        },
        {
          key: "city",
          type: "input",
          templateOptions: {
            label: this.translate.instant("Register.form.city"),
            disabled: true
          }
        },
        {
          key: "country",
          type: "select",
          templateOptions: {
            label: this.translate.instant("Register.form.country"),
            options: COUNTRY_OPTIONS,
            disabled: true
          }
        },
        {
          key: "email",
          type: "input",
          templateOptions: {
            label: this.translate.instant("Register.form.email"),
            disabled: true
          },
          validators: {
            validation: [Validators.email]
          }
        },
        {
          key: "phone",
          type: "input",
          templateOptions: {
            label: this.translate.instant("Register.form.phone"),
            disabled: true
          }
        }
      ]
    }];
  }

  public getUserInformation(): Promise<UserInformation> {

    return new Promise(resolve => {
      var interval = setInterval(() => {
        if (this.websocket.status == 'online') {
          this.service.websocket.sendRequest(new GetUserInformationRequest()).then((response: GetUserInformationResponse) => {
            let user = response.result.user;

            resolve({
              lastname: user.lastname,
              firstname: user.firstname,
              email: user.email,
              phone: user.phone,
              street: user.address.street,
              zip: user.address.zip,
              city: user.address.city,
              country: user.address.country,
            })
          }).catch(() => {
            resolve({
              lastname: "",
              firstname: "",
              email: "",
              phone: "",
              street: "",
              zip: "",
              city: "",
              country: "",
            });
          });
          clearInterval(interval);
        }
      }, 1000)
    });
  }

  /**
   * Logout from OpenEMS Edge or Backend.
   */
  public doLogout() {
    this.websocket.logout();
  }

  public toggleDebugMode(event: CustomEvent) {

    sessionStorage.setItem("DEBUGMODE", event.detail['checked'])
    this.environment.debugMode = event.detail['checked'];
  }

  public setLanguage(language: LanguageTag): void {

    // Get Key of LanguageTag Enum
    localStorage.LANGUAGE = Object.keys(LanguageTag)[Object.values(LanguageTag).indexOf(language)]

    this.service.setLang(LanguageTag[localStorage.LANGUAGE])
    this.websocket.sendRequest(new UpdateUserLanguageRequest({ language: localStorage.LANGUAGE })).then(() => {
      this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
    }).catch((reason) => {
      this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
    });

    this.currentLanguage = language;
    this.translate.use(language);
  }
}
