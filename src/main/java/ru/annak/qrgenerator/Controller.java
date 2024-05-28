package ru.annak.qrgenerator;

import lombok.SneakyThrows;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class Controller {

    @SneakyThrows
    @GetMapping("/ics")
    public ResponseEntity<Resource> ics(@RequestParam(name = "delay", required = false) Integer daysDelay,
                                        @RequestParam(name = "duration", required = false) Integer daysDuration,
                                        @RequestParam(name = "step", required = false) Integer repeatEvery,
                                        @RequestParam(name = "timezone", required = false) String tz,
                                        @RequestParam(name = "summary", required = false) String summary) {

        summary = "Пора принимать БАДы EasyLink";

        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        TimeZone timezone = tz == null ? registry.getTimeZone("Europe/Moscow") : registry.getTimeZone(tz);
        VTimeZone vTimeZone = timezone.getVTimeZone();

        LocalDate startDate = LocalDate.now().plusDays(daysDelay == null ? 0 : daysDelay);
        DateProperty<LocalDate> eventStartDate = new DtStart<>(startDate);
        eventStartDate.add(Value.DATE);

        VEvent event = new VEvent();
        event.add(eventStartDate);
        event.add(new Summary(summary == null ? "Event" : summary));

        UidGenerator ug = new RandomUidGenerator();
        event.add(ug.generateUid());

        int duration = daysDuration == null ? 1 : daysDuration;
        int step = repeatEvery == null ? 1 : repeatEvery;

        Recur recur = new Recur(Recur.DAILY, duration);
        recur.setInterval(step);
        System.out.println(duration + " " + step);

        RRule rule = new RRule(recur);
        event.add(rule);

        Calendar calendar = new Calendar();
        calendar.add(new ProdId("-//Anna//QR//EN"));
        calendar.add(ImmutableVersion.VERSION_2_0);
        calendar.add(ImmutableCalScale.GREGORIAN);

        calendar.add(event);
        calendar.add(vTimeZone);

        File tempFile = File.createTempFile("pills" + ThreadLocalRandom.current().nextInt(0, 1000), ".ics");
        tempFile.deleteOnExit();

        Files.writeString(tempFile.toPath(), calendar.toString());
        Resource fileResource = new FileSystemResource(tempFile);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + fileResource.getFilename())
                .header("Content-Type", "text/calendar")
                .body(fileResource);
    }

}
