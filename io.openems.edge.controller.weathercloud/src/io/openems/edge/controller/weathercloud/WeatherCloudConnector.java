package io.openems.edge.controller.weathercloud;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;

@Designate(ocd = Config.class, factory = true)
@Component(
        name = "Controller.WeatherCloud",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@EventTopics({
        EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
        EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE
})
public class WeatherCloudConnector extends AbstractOpenemsComponent implements EventHandler {

    private static final String WEATHER_CLOUD_BASE_URL = "https://app.weathercloud.net";

    @Reference
    protected ConfigurationAdmin cm;
    private String stationId;

    public WeatherCloudConnector() {
        super(
                OpenemsComponent.ChannelId.values(),
                io.openems.edge.controller.weathercloud.ChannelId.values()
        );
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.stationId = config.weatherCloudStation();
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (!this.isEnabled()) {
            return;
        }
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                break;
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                this.querySunIntensity();
                break;
        }
    }

    public static void main(String[] args) {
        var client = new OkHttpClient();
        var request = new Request.Builder()
                .url(WEATHER_CLOUD_BASE_URL + "/device/values?code=" + "0096876745")
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
        try (var response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String stringResponse = response.body().string();
            var parsedResponse = JsonUtils.parseToJsonObject(stringResponse);
            var sunIntensity = parsedResponse.get("solarrad").getAsFloat();
            System.out.println(sunIntensity);
        } catch (IOException | OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    private void querySunIntensity() {
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
            System.out.println("Got response: " + stringResponse);
            var parsedResponse = JsonUtils.parseToJsonObject(stringResponse);
            var sunIntensity = parsedResponse.get("solarrad").getAsFloat();
            System.out.println("Got solarrad value: " + sunIntensity);
            this.channel(io.openems.edge.controller.weathercloud.ChannelId.SUN_INTENSITY).setNextValue(sunIntensity);
        } catch (IOException | OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

}
