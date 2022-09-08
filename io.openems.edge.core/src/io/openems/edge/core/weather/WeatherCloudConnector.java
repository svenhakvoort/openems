package io.openems.edge.core.weather;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.weather.Weather;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.util.Objects;

import static io.openems.edge.common.weather.Weather.ChannelId.SUN_INTENSITY;

@Designate(ocd = Config.class, factory = false)
@Component(
        name = Weather.SINGLETON_SERVICE_PID,
        immediate = true,
        property = {
                "enabled=true"
        }
)
public class WeatherCloudConnector extends AbstractOpenemsComponent implements Weather, OpenemsComponent {

    private static final String WEATHER_CLOUD_BASE_URL = "https://app.weathercloud.net";

    @Reference
    protected ConfigurationAdmin cm;
    private String stationId;

    public WeatherCloudConnector() {
        super(
                OpenemsComponent.ChannelId.values(),
                Weather.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) {
        super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
        if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
            return;
        }
        this.stationId = config.weatherCloudStation();
    }

    @Modified
    private void modified(ComponentContext context, Config config) {
        super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
        this.stationId = config.weatherCloudStation();
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    private void querySunIntensity() {
        if (this.isEnabled() && !Objects.equals(this.stationId, "")) {
            var client = new OkHttpClient();
            var request = new Request.Builder()
                    .url(WEATHER_CLOUD_BASE_URL + "/device/values?code=" + this.stationId)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build();
            try (var response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String stringResponse = response.body().string();

                var parsedResponse = JsonUtils.parseToJsonObject(stringResponse);
                var sunIntensity = parsedResponse.get("solarrad").getAsFloat();

                this.channel(SUN_INTENSITY).setNextValue(sunIntensity);
            } catch (IOException | OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void updateChannelsBeforeProcessImage() {
        this.querySunIntensity();
    }

}
