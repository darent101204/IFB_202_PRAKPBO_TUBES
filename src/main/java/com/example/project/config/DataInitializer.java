package com.example.project.config;

import com.example.project.model.*;
import com.example.project.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RegionRepository regionRepository;
    private final RtRepository rtRepository;
    private final UserRepository userRepository;
    private final WasteCategoryRepository wasteCategoryRepository;

    @Override
    public void run(String... args) throws Exception {
        // Ensure data consistency for existing users
        userRepository.findAll().forEach(user -> {
            if (user.getIsActive() == null) {
                user.setIsActive(true);
                userRepository.save(user);
            }
        });

        if (userRepository.count() > 0) return;

        // 1. Regions
        Region north = new Region();
        north.setName("North Region");
        north.setDescription("Northern waste collection area");
        regionRepository.save(north);

        Region south = new Region();
        south.setName("South Region");
        south.setDescription("Southern waste collection area");
        regionRepository.save(south);

        // 2. RTs
        Rt rt01 = new Rt();
        rt01.setName("RT 01");
        rt01.setRegion(north);
        rtRepository.save(rt01);

        Rt rt02 = new Rt();
        rt02.setName("RT 02");
        rt02.setRegion(south);
        rtRepository.save(rt02);

        // 3. Waste Categories
        WasteCategory plastic = new WasteCategory();
        plastic.setName("Plastic");
        plastic.setUnit("kg");
        wasteCategoryRepository.save(plastic);

        WasteCategory paper = new WasteCategory();
        paper.setName("Paper");
        paper.setUnit("kg");
        wasteCategoryRepository.save(paper);

        WasteCategory organic = new WasteCategory();
        organic.setName("Organic");
        organic.setUnit("kg");
        wasteCategoryRepository.save(organic);

        // 4. Users
        // Admin
        User admin = new User();
        admin.setName("System Admin");
        admin.setEmail("admin@mail.com");
        admin.setPassword("admin123");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        // RT User
        User rtUser = new User();
        rtUser.setName("RT North Officer");
        rtUser.setEmail("rt@mail.com");
        rtUser.setPassword("rt123");
        rtUser.setRole(Role.RT);
        rtUser.setRegion(north);
        rtUser.setRt(rt01);
        userRepository.save(rtUser);

        // Resident
        User resident = new User();
        resident.setName("John Resident");
        resident.setEmail("resident@mail.com");
        resident.setPassword("resident123");
        resident.setRole(Role.RESIDENT);
        resident.setRegion(north);
        resident.setRt(rt01);
        userRepository.save(resident);

        // Collector
        User collector = new User();
        collector.setName("Mike Collector");
        collector.setEmail("collector@mail.com");
        collector.setPassword("collector123");
        collector.setRole(Role.COLLECTOR);
        collector.setRegion(north);
        userRepository.save(collector);
        
        System.out.println("Initial data seeded successfully.");
    }
}
