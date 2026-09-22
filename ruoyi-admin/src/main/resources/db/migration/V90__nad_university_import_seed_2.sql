-- University catalog import #2: 48 institutions (all China) for partner catalog
-- build-out. DML only -- schema is V9 / V10 / V18 / V46 / V52. Continues the
-- catalog import started in V54 (22 institutions); reference_code picks up
-- wherever V54 left off.
--
-- Every row is inserted as status = ACTIVE, publish_status = DRAFT so nothing
-- reaches the public site until a staff member reviews it and publishes from the
-- admin Universities screen. partner_status = PROSPECT (INTERNAL / staff-only) --
-- these are prospective partners, not confirmed. reference_code continues the
-- NAD-UNI-NNNN sequence from whatever is already in nad_university.
--
-- Data provenance: gathered 2026-09-22 via live web search per institution
-- (official sites, Ministry of Education notices, Wikipedia, ShanghaiRanking /
-- CUAA / Wu Shulian tables) against a name list supplied by staff. Six entries
-- from the original supplied list were dropped rather than guessed at, because
-- the source text had merged two institution names together or gave only a
-- city name with no institution — see docs/DATABASE_DESIGN.md for the list; a
-- follow-up migration can add them once staff confirm the intended names:
--   - "SUFT Central South University of Forestry and Technology" (garbled)
--   - "Guangxi University of Finance and Economics" + "Guangzhou College of
--     Technology and Business" (two names run together, not split with confidence)
--   - "Neusoft Institute of Dalian" + "Neusoft Institute of Guangdong" (two
--     names run together, not split with confidence)
--   - "Tongren" (a city name, not an institution name)
--   - "Guangxi Peixian International College" (unable to confidently identify)
--   - "Guangxi Talent International College" (unable to confidently identify)
--
-- Every remaining fact was checked against a live source before being written;
-- anything that could not be pinned down to a confident source is left NULL or
-- carries an explicit "approximate -- verify" / "CONFIRM" note in the row's
-- remark or ranking note, the same convention V54 used. A few rows correct a
-- factual assumption in the original request brief after verification (noted
-- per-row in `remark`): Wuxi University's real predecessor institution, Nanchang
-- Medical College and Ningbo University of Finance and Economics's PUBLIC/PRIVATE
-- classification, and "Ningbo Tech University" 's actual official English name
-- (Ningbo University of Technology, to avoid confusion with the unrelated
-- NingboTech University / Zhejiang University Ningbo Institute of Technology).
--
-- Left blank for the admin dashboard, same as V54: logo / banner / cover /
-- gallery images, accommodation_info, nearby_info, admissions_email,
-- office_phone, academic departments (nad_department), and most ranking rows.
-- Three entries (Jiangsu Vocational College of Medicine, Mianyang Polytechnic,
-- Ningbo Polytechnic) are associate-degree vocational colleges, not bachelor's
-- universities -- flagged plainly in each introduction/remark so staff can
-- decide whether they belong in this catalog at all.
--
-- Manual rollback (children cascade via FK ON DELETE CASCADE). This removes
-- every 'import'-tagged row from both this migration and V54 -- if only this
-- migration's 48 rows need reverting, filter reference_code to the numeric
-- range this migration actually inserted (check flyway_schema_history /
-- nad_university for the exact NAD-UNI-NNNN boundary V54 left off at):
--   delete from nad_university where create_by = 'import'
--     and reference_code like 'NAD-UNI-%';

set @rc := (select coalesce(max(cast(substring(reference_code, 9) as unsigned)), 0)
            from nad_university where reference_code like 'NAD-UNI-%');

-- ============================================================================
-- 1. Beijing International Studies University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Beijing International Studies University', '北京第二外国语学院',
   'beijing-international-studies-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Beijing', 'Beijing',
   1964, 10700, 900, 680, 'https://www.bisu.edu.cn/', 'Municipal specialist',
   'Beijing International Studies University (BISU) is a Beijing municipal public university in Chaoyang District specialising in foreign languages, international business, tourism management and diplomacy-adjacent fields. It teaches more than twenty foreign languages alongside international relations, law, journalism and hospitality management, and is one of the most internationally oriented universities in Beijing by student exchange volume. BISU is not a Project 211 or 985 institution, but it holds a strong reputation in China for language training, tourism and hospitality management, and for producing graduates who go into diplomacy, translation, tourism administration and international trade.',
   'The university was founded in 1964, established with backing from the Ministry of Foreign Trade and China National Tourism Administration to train interpreters, foreign-trade staff and tourism professionals for China''s external relations. It grew from a specialised language institute into a multi-disciplinary university covering languages, international business, tourism, law and the arts, and adopted its present English name, Beijing International Studies University, as it broadened beyond pure language instruction.',
   'The campus sits in Dingfuzhuang, Chaoyang District, within Beijing''s eastern university belt, with language labs, an interpretation-training centre and a hospitality-management teaching facility reflecting its tourism specialisation. The campus is compact relative to Beijing''s larger comprehensive universities and is well served by the city subway network. Student accommodation is provided in on-campus dormitories.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Total/international student counts and faculty count are approximate -- verify against the current admissions bulletin before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Beijing International Studies University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Beijing municipal public university specialising in foreign languages and international business'),
 (@u, 'HIGHLIGHT', 2,  'Teaches more than twenty foreign languages alongside international trade, law and journalism'),
 (@u, 'HIGHLIGHT', 3,  'Strong national reputation in tourism management and hospitality administration'),
 (@u, 'HIGHLIGHT', 4,  'Founded in 1964 to train interpreters and foreign-trade specialists'),
 (@u, 'HIGHLIGHT', 5,  'Located in Chaoyang District, Beijing''s eastern university and diplomatic-missions belt'),
 (@u, 'HIGHLIGHT', 6,  'High proportion of international students relative to total enrolment'),
 (@u, 'HIGHLIGHT', 7,  'Dedicated interpretation-training centre and language laboratories'),
 (@u, 'HIGHLIGHT', 8,  'Graduate pipeline into diplomacy, translation and international trade roles'),
 (@u, 'HIGHLIGHT', 9,  'Close historical ties to China''s tourism administration and foreign-trade ministries'),
 (@u, 'HIGHLIGHT', 10, 'Compact campus in a well-connected part of eastern Beijing'),
 (@u, 'ADVANTAGE', 11, 'Direct access to Beijing''s diplomatic, media and international-business networks'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting translation, tourism or international-trade careers'),
 (@u, 'ADVANTAGE', 13, 'Established international-student intake with dedicated support services'),
 (@u, 'ADVANTAGE', 14, 'Subway access to central Beijing and major diplomatic and business districts'),
 (@u, 'ADVANTAGE', 15, 'Smaller, specialised institution rather than a large comprehensive university'),
 (@u, 'ADVANTAGE', 16, 'Chinese-language and language-pair programmes suited to non-native speakers'),
 (@u, 'ADVANTAGE', 17, 'Strong employer links in tourism, hospitality and foreign-trade sectors'),
 (@u, 'ADVANTAGE', 18, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 19, 'Beijing location offers internship access to embassies, media and trade bodies'),
 (@u, 'ADVANTAGE', 20, 'Long-standing focus on practical interpretation and cross-cultural communication skills');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ShanghaiRanking BCUR (Language)', 7, 2026, 'Subject-specific ranking of language-focused universities, not the overall BCUR table -- verify');

-- ============================================================================
-- 2. China University of Petroleum (East China)
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('China University of Petroleum (East China)', '中国石油大学（华东）',
   'china-university-of-petroleum-east-china', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Qingdao', 'Shandong',
   1953, 26000, 1500, 2600, 'https://www.upc.edu.cn/', 'Double First-Class',
   'China University of Petroleum (East China) is a national key university in Qingdao, Shandong, and a Project 211 and Double First-Class institution under the Ministry of Education. It is China''s leading university for petroleum and petrochemical engineering, geology and related energy disciplines, and also teaches mechanical, chemical, computer, materials, economics and management fields. This is the independently administered Qingdao (East China) campus, distinct from China University of Petroleum (Beijing) -- the two split into separately run universities after originating from the same parent institution, and should not be treated as branches of one another.',
   'The university traces its founding to the Beijing Petroleum Institute, established in 1953 to train engineers for China''s emerging oil and gas industry. In 1969 the institute relocated to Dongying, Shandong, becoming the East China campus, and the Beijing and East China operations were later formalised as separate universities. Relocation of the main campus to Qingdao began in 2004 and completed in 2012, after which the university adopted its present form as China University of Petroleum (East China), retaining a secondary campus in Dongying.',
   'The main Qingdao campus sits in the city''s university and high-tech district, with laboratories for drilling, reservoir engineering, petrochemical processing and offshore-energy research, plus a secondary campus in Dongying near the Shengli oilfield. Facilities support close collaboration with national oil companies on applied research. Student accommodation is provided in on-campus dormitories, with the Qingdao coastal setting distinguishing it from most other Chinese petroleum-engineering schools.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Do not confuse with China University of Petroleum (Beijing) -- separate institution, separate catalog entry if added. Enrolment, international-student count and faculty count are approximate -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'China University of Petroleum (East China)' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'National key university, Project 211 and Double First-Class institution'),
 (@u, 'HIGHLIGHT', 2,  'China''s leading university for petroleum and petrochemical engineering'),
 (@u, 'HIGHLIGHT', 3,  'Independently run Qingdao (East China) campus, distinct from the Beijing campus'),
 (@u, 'HIGHLIGHT', 4,  'Strong geology, energy-engineering, chemical and materials-science programmes'),
 (@u, 'HIGHLIGHT', 5,  'Deep research ties to national oil and gas companies'),
 (@u, 'HIGHLIGHT', 6,  'Coastal Qingdao main campus plus a secondary Dongying campus near the Shengli oilfield'),
 (@u, 'HIGHLIGHT', 7,  'Founded in 1953 as the Beijing Petroleum Institute'),
 (@u, 'HIGHLIGHT', 8,  'Completed relocation to Qingdao in 2012'),
 (@u, 'HIGHLIGHT', 9,  'Large undergraduate and postgraduate population across engineering and science'),
 (@u, 'HIGHLIGHT', 10, 'Laboratories for drilling, reservoir engineering and offshore-energy research'),
 (@u, 'ADVANTAGE', 11, 'Coastal Qingdao location, more temperate and internationally connected than inland energy schools'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting energy, petrochemical and geoscience careers'),
 (@u, 'ADVANTAGE', 13, 'Direct recruitment pipeline into national and international oil and gas companies'),
 (@u, 'ADVANTAGE', 14, 'Project 211 status gives strong research funding and lab infrastructure'),
 (@u, 'ADVANTAGE', 15, 'Qingdao offers transport links to Beijing, Shanghai and international shipping routes'),
 (@u, 'ADVANTAGE', 16, 'English-taught postgraduate programmes in engineering fields (verify current list)'),
 (@u, 'ADVANTAGE', 17, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 18, 'Lower cost of living than Beijing or Shanghai while retaining national-university resources'),
 (@u, 'ADVANTAGE', 19, 'Strong alumni network across China''s energy sector'),
 (@u, 'ADVANTAGE', 20, 'Modern campus built out during the 2004-2012 Qingdao relocation');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ShanghaiRanking BCUR', 73, 2026, 'Approximate -- verify against current edition before publishing'),
 (@u, 'ARWU / Shanghai Ranking (global)', 301, 2025, 'Approximate band (301-400) -- verify');

-- ============================================================================
-- 3. Dalian Polytechnic University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Dalian Polytechnic University', '大连工业大学',
   'dalian-polytechnic-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Dalian', 'Liaoning',
   1958, 18000, NULL, 1300, 'https://www.dlpu.edu.cn/', 'Provincial undergrad',
   'Dalian Polytechnic University is a provincial public university in Dalian, Liaoning, known for food science and engineering, textiles and fashion design, light-industry and chemical engineering, and art and design. It is a mid-sized multi-disciplinary teaching university that also covers management, economics, foreign languages and information technology, and is a recognised national base for food-processing and packaging-engineering research. It is not a Project 211 or 985 institution but has a strong regional reputation in its specialist fields.',
   'The university was founded in 1958 in Shenyang as the Shenyang Institute of Light Industry. In 1970 the institute relocated to Dalian and was renamed the Dalian Institute of Light Industry, reflecting its focus on light-industrial engineering, food processing and textiles. It gained full university status and adopted its present name, Dalian Polytechnic University, in 2006.',
   'The main campus is in Dalian, a coastal city in Liaoning province, with dedicated laboratories for food science, textile and garment engineering, packaging engineering and industrial design. The university also maintains a secondary teaching site within the city. Student accommodation is provided in on-campus dormitories, and the campus benefits from Dalian''s status as a major port and industrial city in northeast China.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Enrolment and faculty count are approximate -- verify before publishing. No major-table ranking found for this institution; add manually if one appears in a later edition. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Dalian Polytechnic University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public university with a national reputation in food science and engineering'),
 (@u, 'HIGHLIGHT', 2,  'Strong textile, fashion-design and light-industry engineering programmes'),
 (@u, 'HIGHLIGHT', 3,  'Recognised national base for food-processing and packaging-engineering research'),
 (@u, 'HIGHLIGHT', 4,  'Founded in 1958 as the Shenyang Institute of Light Industry'),
 (@u, 'HIGHLIGHT', 5,  'Relocated to Dalian in 1970, gained university status in 2006'),
 (@u, 'HIGHLIGHT', 6,  'Located in Dalian, a major coastal port city in Liaoning province'),
 (@u, 'HIGHLIGHT', 7,  'Multi-disciplinary: engineering, food science, art and design, management, languages'),
 (@u, 'HIGHLIGHT', 8,  'Dedicated laboratories for textile, garment and industrial design'),
 (@u, 'HIGHLIGHT', 9,  'Close ties to Liaoning''s light-industry and food-processing sector'),
 (@u, 'HIGHLIGHT', 10, 'Bachelor and selected master programmes with degree-granting authority'),
 (@u, 'ADVANTAGE', 11, 'Dalian offers a temperate coastal climate and strong port-city economy'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting food science, textiles or design careers'),
 (@u, 'ADVANTAGE', 13, 'Lower cost of living than Beijing or Shanghai'),
 (@u, 'ADVANTAGE', 14, 'Regional employer network in Liaoning''s food, textile and light-industry sectors'),
 (@u, 'ADVANTAGE', 15, 'Dalian is a major transport hub with rail and air links across northeast China'),
 (@u, 'ADVANTAGE', 16, 'Established international-student intake channel'),
 (@u, 'ADVANTAGE', 17, 'Applied, practice-oriented curriculum aligned with local industry needs'),
 (@u, 'ADVANTAGE', 18, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 19, 'Smaller class sizes than Dalian''s larger comprehensive universities'),
 (@u, 'ADVANTAGE', 20, 'Responsive to partner-managed recruitment given its regional focus');
-- No ranking rows: institution is not present in major ranking tables. Add in dashboard if one appears.

-- ============================================================================
-- 4. Dalian University of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Dalian University of Technology', '大连理工大学',
   'dalian-university-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Dalian', 'Liaoning',
   1949, 39000, 2000, 3400, 'https://www.dlut.edu.cn/', '985/211 (Class A)',
   'Dalian University of Technology (DUT) is an elite national key university directly administered by the Ministry of Education, and one of China''s Project 985 and Project 211 universities -- placing it in the top tier of Chinese higher education alongside institutions such as Tsinghua and Peking University, well above the ordinary provincial-university tier of most catalog entries. It was selected into Category A of the Double First-Class Initiative in 2017. DUT is a comprehensive research university with particular strength in engineering, chemistry, naval architecture and ocean engineering, computer science and management, and it consistently ranks among the top 30-40 universities in China.',
   'The university was founded in 1949, shortly after the establishment of the People''s Republic of China, as a comprehensive engineering-focused institution in the port city of Dalian. It was designated a Project 211 university in 1996 and a Project 985 university in 2001, with the Ministry of Education, Liaoning Province and Dalian City jointly funding its development. In 2017 it was named a Double First-Class Class A university, with several of its engineering and science disciplines recognised as world-class.',
   'The main Linggong Road campus in Ganjingzi District, Dalian, is a large research campus with extensive engineering laboratories, a naval-architecture and ocean-engineering test facility, and modern science buildings; the university also operates a Kaifaqu (development-zone) campus and a Panjin campus in Liaoning. Student accommodation is provided in on-campus dormitories, and Dalian''s coastal setting and status as a major economic and shipping centre give students strong access to internships and research partnerships.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. This is an elite Project 985/211 university -- expect highly selective admission and competitive partnership terms, unlike most other catalog entries in this batch. Enrolment, international-student count and faculty count are approximate -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Dalian University of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Elite national key university, Project 985 and Project 211'),
 (@u, 'HIGHLIGHT', 2,  'Double First-Class Class A institution (2017) -- top tier of Chinese higher education'),
 (@u, 'HIGHLIGHT', 3,  'Nationally leading in engineering, chemistry and naval/ocean engineering'),
 (@u, 'HIGHLIGHT', 4,  'Founded in 1949, one of the first comprehensive engineering universities of the PRC'),
 (@u, 'HIGHLIGHT', 5,  'Consistently ranks among the top 30-40 universities in China'),
 (@u, 'HIGHLIGHT', 6,  'Large research campus in Ganjingzi District, Dalian, plus Kaifaqu and Panjin campuses'),
 (@u, 'HIGHLIGHT', 7,  'Directly administered by the Ministry of Education'),
 (@u, 'HIGHLIGHT', 8,  'Strong computer science, management and materials-science programmes'),
 (@u, 'HIGHLIGHT', 9,  'Significant international research collaboration and exchange programmes'),
 (@u, 'HIGHLIGHT', 10, 'Extensive engineering and naval-architecture test facilities on campus'),
 (@u, 'ADVANTAGE', 11, 'Elite-tier degree with strong recognition among Chinese and international employers'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting engineering, science or naval/ocean-engineering careers'),
 (@u, 'ADVANTAGE', 13, 'Dalian is a major coastal economic hub with strong internship and employment access'),
 (@u, 'ADVANTAGE', 14, 'Established English-taught programme offering for international students (verify current list)'),
 (@u, 'ADVANTAGE', 15, 'Chinese Government and university scholarships available (verify current terms)'),
 (@u, 'ADVANTAGE', 16, 'Strong research funding and lab infrastructure from Project 985/211 status'),
 (@u, 'ADVANTAGE', 17, 'Global academic partnerships supporting exchange and joint-degree options'),
 (@u, 'ADVANTAGE', 18, 'Dalian offers a temperate coastal climate distinct from many inland engineering schools'),
 (@u, 'ADVANTAGE', 19, 'Large, well-resourced alumni network across Chinese industry and academia'),
 (@u, 'ADVANTAGE', 20, 'Recognised international brand that strengthens graduate mobility');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ShanghaiRanking BCUR', 28, 2024, 'Approximate -- verify against current edition before publishing'),
 (@u, 'QS World University Rankings', 463, 2027, 'Approximate -- verify against current edition'),
 (@u, 'US News Best Global Universities', 254, 2025, 'Approximate -- verify');

-- ============================================================================
-- 5. Guangdong University of Foreign Studies
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Guangdong University of Foreign Studies', '广东外语外贸大学',
   'guangdong-university-of-foreign-studies', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Guangzhou', 'Guangdong',
   1965, 32000, 2500, 1900, 'https://www.gdufs.edu.cn/', 'Provincial key/language',
   'Guangdong University of Foreign Studies (GDUFS) is a provincial key public university in Guangzhou and one of China''s leading institutions for foreign languages, international trade, business and law. It teaches more than twenty foreign languages alongside economics, international business, law, journalism and public administration, and is widely regarded as one of China''s top language-and-trade universities, comparable in reputation to Beijing Foreign Studies University and Shanghai International Studies University. Its location in Guangdong -- China''s largest trading province and a hub for Hong Kong, Macau and Greater Bay Area business -- gives it a strong practical orientation toward international commerce.',
   'The university''s lineage traces to the Guangzhou Foreign Languages Institute, founded in 1965, and the Guangzhou Institute of Foreign Trade, founded in 1980. The two institutions merged in May 1995 to form Guangdong University of Foreign Studies, combining language education with international-trade and business training. The founding year used in this catalog reflects the earlier predecessor institute; the formal merger creating the present university took place in 1995 -- verify which date the university''s own materials cite as authoritative before publishing.',
   'The university operates two main campuses in Guangzhou -- Baiyun Mountain (Beishan) and Guangzhou University Town (Daxuecheng) -- with language laboratories, an interpretation-training centre, and dedicated facilities for international-trade simulation and negotiation training. Student accommodation is provided in on-campus dormitories at both campuses, and Guangzhou''s role as a major trade and logistics hub gives students strong access to internships with import-export and multinational companies.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Founding year is ambiguous between the 1965 predecessor institute and the 1995 formal merger -- verify against GDUFS''s own official history page before publishing. Enrolment, international-student count and faculty count are approximate -- verify. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Guangdong University of Foreign Studies' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial key university and one of China''s leading language-and-trade institutions'),
 (@u, 'HIGHLIGHT', 2,  'Teaches more than twenty foreign languages alongside international trade and law'),
 (@u, 'HIGHLIGHT', 3,  'Formed in 1995 from the merger of a foreign-languages institute and a foreign-trade college'),
 (@u, 'HIGHLIGHT', 4,  'Two campuses in Guangzhou: Baiyun Mountain and University Town'),
 (@u, 'HIGHLIGHT', 5,  'Located in Guangdong, China''s largest trading province and gateway to the Greater Bay Area'),
 (@u, 'HIGHLIGHT', 6,  'Strong international-business, economics and public-administration programmes'),
 (@u, 'HIGHLIGHT', 7,  'Dedicated interpretation-training centre and trade-negotiation simulation facilities'),
 (@u, 'HIGHLIGHT', 8,  'Large international-student population relative to comparable universities'),
 (@u, 'HIGHLIGHT', 9,  'Reputation comparable to Beijing Foreign Studies University and Shanghai International Studies University'),
 (@u, 'HIGHLIGHT', 10, 'Strong employer links across import-export, multinational and diplomatic sectors'),
 (@u, 'ADVANTAGE', 11, 'Guangzhou''s trade-hub economy gives direct access to internships and graduate jobs'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting translation, international trade or diplomacy careers'),
 (@u, 'ADVANTAGE', 13, 'Proximity to Hong Kong and Macau broadens internship and exchange options'),
 (@u, 'ADVANTAGE', 14, 'Established, large-scale international-student support infrastructure'),
 (@u, 'ADVANTAGE', 15, 'Guangzhou has extensive high-speed rail and air links across the Greater Bay Area'),
 (@u, 'ADVANTAGE', 16, 'Chinese-language and language-pair programmes suited to non-native speakers'),
 (@u, 'ADVANTAGE', 17, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 18, 'Milder subtropical climate than northern Chinese universities'),
 (@u, 'ADVANTAGE', 19, 'Strong alumni network across trade, diplomacy and multinational business'),
 (@u, 'ADVANTAGE', 20, 'Two-campus setup gives access to both a heritage city site and a modern university-town district');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ShanghaiRanking BCUR (Language)', 6, 2026, 'Subject-specific ranking of language-focused universities, not the overall BCUR table -- verify');

-- ============================================================================
-- 6. Hangzhou City University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hangzhou City University', '杭州城市大学',
   'hangzhou-city-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Hangzhou', 'Zhejiang',
   1999, 13000, NULL, 900, 'https://www.hzcu.edu.cn/', 'Provincial (new)',
   'Hangzhou City University is a public university in Gongshu District, Hangzhou, offering programmes across economics, law, liberal arts, history, science, engineering, medicine, management and art through thirteen schools and one Sino-foreign joint institute. It is a relatively young, newly independent institution -- it carries the lineage and academic culture of its founding partner, Zhejiang University, but is now a separately administered public university rather than a Zhejiang University college.',
   'The university''s predecessor, Zhejiang University City College, was jointly founded in July 1999 by Zhejiang University and the Hangzhou municipal government as an "independent college" model common in China at the time, combining a parent university''s academic resources with local government funding. In January 2020, the Ministry of Education approved the restructuring of Zhejiang University City College into an independently run public university, Hangzhou City University, revoking the former college''s dependent status. In August 2022 the university was further approved as a master''s-degree-granting institution, a milestone that is sometimes cited alongside the earlier 2020 restructuring -- verify the precise sequence against the university''s official history page before publishing.',
   'The campus is located at Huzhou Street in Gongshu District, Hangzhou, covering roughly 68 hectares, with modern teaching buildings reflecting its recent transition to independent-university status. Student accommodation is provided in on-campus dormitories, and the Gongshu District location gives ready access to central Hangzhou and the city''s technology and e-commerce industry corridor.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Public sources give two milestone dates (Jan 2020 restructuring vs Aug 2022 master''s-granting approval) that are sometimes conflated as "the 2022 renaming" -- verify the exact sequence and date against HZCU''s own official history before publishing. Enrolment and faculty count are approximate -- verify. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Hangzhou City University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Newly independent public university, formerly Zhejiang University City College'),
 (@u, 'HIGHLIGHT', 2,  'Restructured into an independent university by Ministry of Education approval in January 2020'),
 (@u, 'HIGHLIGHT', 3,  'Approved as a master''s-degree-granting institution in August 2022'),
 (@u, 'HIGHLIGHT', 4,  'Thirteen schools plus one Sino-foreign joint institute'),
 (@u, 'HIGHLIGHT', 5,  'Founded in 1999 as a joint venture between Zhejiang University and Hangzhou city government'),
 (@u, 'HIGHLIGHT', 6,  'Programmes across economics, law, liberal arts, science, engineering, medicine, management and art'),
 (@u, 'HIGHLIGHT', 7,  'Located in Gongshu District, Hangzhou, within the city''s technology and e-commerce corridor'),
 (@u, 'HIGHLIGHT', 8,  'Academic culture and founding lineage tied to Zhejiang University'),
 (@u, 'HIGHLIGHT', 9,  'Compact, modern campus of roughly 68 hectares'),
 (@u, 'HIGHLIGHT', 10, 'Growing postgraduate offering following its 2022 master''s-granting approval'),
 (@u, 'ADVANTAGE', 11, 'Hangzhou''s technology and e-commerce economy (Alibaba, digital-economy firms) close by'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants who want a Zhejiang University-linked academic culture in a smaller setting'),
 (@u, 'ADVANTAGE', 13, 'Central Hangzhou location with strong transport links'),
 (@u, 'ADVANTAGE', 14, 'Younger, growing institution with recent investment in facilities and postgraduate programmes'),
 (@u, 'ADVANTAGE', 15, 'Lower entry competitiveness than Zhejiang University itself'),
 (@u, 'ADVANTAGE', 16, 'Access to Hangzhou''s scenic and cultural amenities, including West Lake'),
 (@u, 'ADVANTAGE', 17, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 18, 'Emerging Sino-foreign joint-institute pathway for internationally minded students'),
 (@u, 'ADVANTAGE', 19, 'Strong regional employer network in Zhejiang''s digital and manufacturing economy'),
 (@u, 'ADVANTAGE', 20, 'Responsive, growth-oriented institution given its recent independence');
-- No ranking rows: institution is too recently independent to appear in major ranking tables under its current name. Add in dashboard if one appears.

-- ============================================================================
-- 7. Hangzhou Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hangzhou Normal University', '杭州师范大学',
   'hangzhou-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Hangzhou', 'Zhejiang',
   1908, 22000, 2000, 1900, 'https://www.hznu.edu.cn/', 'Provincial key',
   'Hangzhou Normal University (HZNU) is a provincial key public university in Hangzhou, Zhejiang, with roots as a teacher-training institution and a present-day profile spanning education, medicine, science, engineering, humanities, arts and business. It is well known in China as the alma mater of Jack Ma and for its Alibaba Business School, alongside long-standing strength in teacher education and, more recently, biomedical and materials research. HZNU is not a Project 211 or 985 institution but holds several nationally recognised disciplines and has a large international-student population for a regional normal university.',
   'The university was founded in 1908 as a teacher-training school in Hangzhou, one of the earlier modern teacher-education institutions in Zhejiang province. Through the twentieth century it expanded and merged with several other local institutions, broadening from pure teacher training into a comprehensive university, and adopted its present name, Hangzhou Normal University, as it gained university status. It has since grown a significant medical school and, since the 2010s, a business school built around its connection to Hangzhou-based Alibaba Group.',
   'The main campus is located in Cangqian, Yuhang District, Hangzhou, with additional teaching sites in the city, including medical-school and business-school facilities. The campus supports education, medicine, science, engineering, arts and business disciplines with dedicated laboratories and teaching hospitals affiliated with the medical school. Student accommodation is provided in on-campus dormitories, and the Hangzhou location gives access to the city''s technology and digital-economy sector.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Enrolment, international-student count and faculty count are approximate -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Hangzhou Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial key university with roots in teacher education since 1908'),
 (@u, 'HIGHLIGHT', 2,  'Home to the Alibaba Business School and notable alumnus Jack Ma'),
 (@u, 'HIGHLIGHT', 3,  'Comprehensive profile: education, medicine, science, engineering, humanities, business'),
 (@u, 'HIGHLIGHT', 4,  'Significant medical school with affiliated teaching hospitals'),
 (@u, 'HIGHLIGHT', 5,  'Large international-student population for a regional normal university'),
 (@u, 'HIGHLIGHT', 6,  'Main campus in Cangqian, Yuhang District, Hangzhou'),
 (@u, 'HIGHLIGHT', 7,  'Long-standing strength in teacher training and education research'),
 (@u, 'HIGHLIGHT', 8,  'Growing biomedical and materials-science research output'),
 (@u, 'HIGHLIGHT', 9,  'Business school built around ties to the Hangzhou-based digital economy'),
 (@u, 'HIGHLIGHT', 10, 'Multiple nationally recognised disciplines despite non-211/985 status'),
 (@u, 'ADVANTAGE', 11, 'Direct academic and networking connection to Hangzhou''s digital-economy and startup scene'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting education, business or medical careers'),
 (@u, 'ADVANTAGE', 13, 'Established, large-scale international-student support infrastructure'),
 (@u, 'ADVANTAGE', 14, 'Hangzhou offers strong transport links via high-speed rail and an international airport'),
 (@u, 'ADVANTAGE', 15, 'Access to Hangzhou''s scenic and cultural amenities, including West Lake'),
 (@u, 'ADVANTAGE', 16, 'English-taught programmes in selected fields (verify current list)'),
 (@u, 'ADVANTAGE', 17, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 18, 'Strong regional employer network in education, healthcare and the digital economy'),
 (@u, 'ADVANTAGE', 19, 'Lower cost of living than Shanghai or Beijing while remaining close to both'),
 (@u, 'ADVANTAGE', 20, 'Reputable teaching-hospital network for medicine and health-sciences students');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ShanghaiRanking BCUR', 120, 2024, 'Approximate -- verify against current edition before publishing'),
 (@u, 'ARWU / Shanghai Ranking (global)', 501, 2025, 'Approximate band (501-600) -- verify');

-- ============================================================================
-- 8. Hanjiang Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hanjiang Normal University', '汉江师范学院',
   'hanjiang-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Shiyan', 'Hubei',
   1904, 15300, NULL, 900, 'https://www.hjnu.edu.cn/', 'Provincial undergrad',
   'Hanjiang Normal University is a provincial public university in Shiyan, Hubei, focused primarily on teacher education alongside science, engineering, management and arts programmes. It is a smaller regional normal university serving Hubei''s northwest, with a long institutional lineage but a relatively recent promotion to full undergraduate-university status. It is not a Project 211 or 985 institution and does not currently appear in major international ranking tables.',
   'The institution''s lineage traces back to Yunshan Academy, a Ming-dynasty-era academy, and more directly to the Hubei Yunyang Teacher Training School, established in 1904 in what is now Shiyan. After decades as a normal (teacher-training) school and college, it was upgraded to a full undergraduate institution and renamed Hanjiang Normal University on 22 March 2016, reflecting its expansion beyond a purely teacher-training remit.',
   'The university operates two campuses in Shiyan -- the main Shiyan campus and a Danjiang campus -- together covering a substantial area, with facilities supporting teacher-education, science and engineering programmes. Student accommodation is provided in on-campus dormitories. Shiyan is a mid-sized industrial city in northwest Hubei, known as a base for China''s automotive industry.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Enrolment and faculty count are approximate -- verify before publishing. No major-table ranking found for this institution; add manually if one appears in a later edition. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Hanjiang Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public university focused on teacher education'),
 (@u, 'HIGHLIGHT', 2,  'Institutional lineage traces to a Ming-dynasty academy and an 1904 teacher-training school'),
 (@u, 'HIGHLIGHT', 3,  'Upgraded to full undergraduate-university status and renamed in March 2016'),
 (@u, 'HIGHLIGHT', 4,  'Two campuses in Shiyan: the main campus and a Danjiang campus'),
 (@u, 'HIGHLIGHT', 5,  'Programmes across education, science, engineering, management and arts'),
 (@u, 'HIGHLIGHT', 6,  'Located in Shiyan, a mid-sized industrial city and automotive-industry base'),
 (@u, 'HIGHLIGHT', 7,  'Serves as a key regional teacher-training pipeline for northwest Hubei'),
 (@u, 'HIGHLIGHT', 8,  'Full-time student body of over 15,000'),
 (@u, 'HIGHLIGHT', 9,  'Long institutional history spanning over a century'),
 (@u, 'HIGHLIGHT', 10, 'Bachelor programmes with degree-granting authority across its teaching disciplines'),
 (@u, 'ADVANTAGE', 11, 'Lower cost of living than provincial-capital universities'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting teacher-education or regional public-sector careers'),
 (@u, 'ADVANTAGE', 13, 'Smaller class sizes than larger comprehensive Hubei universities'),
 (@u, 'ADVANTAGE', 14, 'Regional employer network in Shiyan''s automotive and manufacturing industries'),
 (@u, 'ADVANTAGE', 15, 'Two-campus setup with dedicated teacher-education facilities'),
 (@u, 'ADVANTAGE', 16, 'Growing openness to international-student recruitment'),
 (@u, 'ADVANTAGE', 17, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 18, 'Supportive environment for Chinese-language study alongside a degree'),
 (@u, 'ADVANTAGE', 19, 'Rail links connecting Shiyan to Wuhan and other Hubei cities'),
 (@u, 'ADVANTAGE', 20, 'Responsive to partner-managed recruitment given its recent university-status upgrade');
-- No ranking rows: institution is not present in major ranking tables. Add in dashboard if one appears.

-- ============================================================================
-- 9. Harbin Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Harbin Normal University', '哈尔滨师范大学',
   'harbin-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Harbin', 'Heilongjiang',
   1951, 32000, 1000, 2200, 'https://www.hrbnu.edu.cn/', 'Provincial key',
   'Harbin Normal University (HRBNU) is a provincial key public university in Harbin, Heilongjiang, and one of the largest teacher-education institutions in northeast China. Alongside teacher education it covers science, engineering, humanities, music, fine arts, history and Slavic-language studies, reflecting Harbin''s historical Russian influence. It is a large comprehensive normal university with one of the bigger enrolments among China''s provincial teacher-training universities, though it is not a Project 211 or 985 institution.',
   'The university was established in 1951 in Harbin, in the early years of the People''s Republic of China, as a teacher-training institution for Heilongjiang province. Over subsequent decades it merged with and absorbed several other local colleges, expanding from a pure teacher-training school into a comprehensive normal university spanning science, humanities, arts and languages, while retaining teacher education as a core mission.',
   'The university operates multiple campuses across Harbin, including facilities dedicated to music, fine arts and Slavic-language instruction reflecting the city''s Russian architectural and cultural heritage. Student accommodation is provided in on-campus dormitories. Harbin''s cold-climate northeastern setting and historical ties to Russia give the university a distinctive character among China''s normal universities.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. International-student count and faculty count are approximate -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Harbin Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial key university and one of the largest teacher-education institutions in northeast China'),
 (@u, 'HIGHLIGHT', 2,  'Founded in 1951, expanded through mergers into a comprehensive normal university'),
 (@u, 'HIGHLIGHT', 3,  'Strong Slavic-language studies reflecting Harbin''s Russian cultural heritage'),
 (@u, 'HIGHLIGHT', 4,  'Notable music and fine-arts schools alongside teacher education'),
 (@u, 'HIGHLIGHT', 5,  'Large full-time enrolment of around 32,000 students'),
 (@u, 'HIGHLIGHT', 6,  'Multiple campuses across Harbin, Heilongjiang''s provincial capital'),
 (@u, 'HIGHLIGHT', 7,  'Broad disciplinary coverage: science, engineering, humanities, arts, languages'),
 (@u, 'HIGHLIGHT', 8,  'Key regional teacher-training pipeline for Heilongjiang province'),
 (@u, 'HIGHLIGHT', 9,  'Long institutional history dating to the early PRC period'),
 (@u, 'HIGHLIGHT', 10, 'Distinctive cold-climate northeastern China setting'),
 (@u, 'ADVANTAGE', 11, 'Lower cost of living than universities in southern or coastal Chinese cities'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting teacher education, Russian-language study or the arts'),
 (@u, 'ADVANTAGE', 13, 'Distinctive Russian-influenced architecture and culture in Harbin'),
 (@u, 'ADVANTAGE', 14, 'Regional employer network across Heilongjiang''s education and public sector'),
 (@u, 'ADVANTAGE', 15, 'Large, well-established international-student support infrastructure'),
 (@u, 'ADVANTAGE', 16, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 17, 'Harbin''s well-known winter tourism and ice-festival season'),
 (@u, 'ADVANTAGE', 18, 'Rail and air links connecting Harbin to Beijing, Shanghai and Russia'),
 (@u, 'ADVANTAGE', 19, 'Strong Slavic-studies pathway for students interested in Russia-China relations'),
 (@u, 'ADVANTAGE', 20, 'Long-established alumni network across northeast China''s education sector');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ShanghaiRanking BCUR', 223, 2026, 'Approximate -- verify against current edition before publishing');

-- ============================================================================
-- 10. Hebei University of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hebei University of Technology', '河北工业大学',
   'hebei-university-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Tianjin', 'Hebei',
   1903, 30382, 800, 2200, 'https://www.hebut.edu.cn/', 'Project 211',
   'Hebei University of Technology (HEBUT) is a Project 211 national key university with a notable geographic quirk: although it is Hebei province''s flagship engineering university and administratively affiliated with Hebei, its main campus is physically located in Tianjin municipality, not within Hebei province itself. This reflects its origin in Tianjin during the late Qing dynasty and a series of historical boundary and administrative changes; since 2014 the university has been jointly built by Hebei Province, Tianjin Municipality and the Ministry of Education. This entry deliberately records city = Tianjin and province = Hebei to reflect that reality rather than an error. The university is strong in electrical engineering, materials science, mechanical engineering and chemical engineering.',
   'The university''s predecessor, the Beiyang Institute of Science and Technology (Beiyang Gongyi Xuetang), was founded in Tianjin in 1903, during the late Qing dynasty''s push to modernise technical education. Through numerous mergers, relocations and renamings over the twentieth century, the institution eventually became Hebei University of Technology, retaining its historical campus location in Tianjin even as its provincial affiliation shifted to Hebei following province boundary and administrative reorganisations. It was named a Project 211 national key university in 1996, and since 2014 has been jointly developed by Hebei Province, Tianjin Municipality and the Ministry of Education.',
   'The main campus is in Tianjin, with additional teaching facilities in Langfang, Hebei province. Facilities include electrical-engineering and materials-science laboratories reflecting the university''s traditional strengths. Student accommodation is provided in on-campus dormitories, and the Tianjin campus location gives students access to one of China''s major northern port and industrial cities despite the university''s formal Hebei provincial affiliation.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. IMPORTANT: city=Tianjin, province=Hebei is intentional, not an error -- the university''''s main campus is physically in Tianjin while it remains a Hebei provincial Project 211 university; do not "correct" this without checking this note first. Faculty/international-student counts approximate -- verify. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Hebei University of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Project 211 national key university'),
 (@u, 'HIGHLIGHT', 2,  'Hebei province''s flagship engineering university, though its main campus is in Tianjin'),
 (@u, 'HIGHLIGHT', 3,  'Jointly built by Hebei Province, Tianjin Municipality and the Ministry of Education since 2014'),
 (@u, 'HIGHLIGHT', 4,  'Founded in 1903 as the Beiyang Institute of Science and Technology'),
 (@u, 'HIGHLIGHT', 5,  'Strong electrical engineering, materials science and mechanical/chemical engineering programmes'),
 (@u, 'HIGHLIGHT', 6,  'Named a Project 211 university in 1996'),
 (@u, 'HIGHLIGHT', 7,  'Main campus in Tianjin plus a teaching site in Langfang, Hebei'),
 (@u, 'HIGHLIGHT', 8,  'Total enrolment of over 30,000 undergraduate and postgraduate students'),
 (@u, 'HIGHLIGHT', 9,  'Over a century of continuous institutional history'),
 (@u, 'HIGHLIGHT', 10, 'Recognised nationally for electrical-engineering and materials-science research'),
 (@u, 'ADVANTAGE', 11, 'Project 211 status gives strong research funding and international recognition'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting electrical, materials or mechanical engineering careers'),
 (@u, 'ADVANTAGE', 13, 'Tianjin campus location gives access to a major northern port and industrial city'),
 (@u, 'ADVANTAGE', 14, 'Lower entry competitiveness than Tianjin''s other Project 211/985 universities'),
 (@u, 'ADVANTAGE', 15, 'Strong regional employer network across Hebei and Tianjin industry'),
 (@u, 'ADVANTAGE', 16, 'Tianjin''s high-speed rail links to Beijing (under 40 minutes)'),
 (@u, 'ADVANTAGE', 17, 'English-taught programmes in selected engineering fields (verify current list)'),
 (@u, 'ADVANTAGE', 18, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 19, 'Established international-student intake channel'),
 (@u, 'ADVANTAGE', 20, 'Dual provincial/municipal backing (Hebei and Tianjin) broadens funding and partnership opportunities');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ShanghaiRanking BCUR', 109, 2024, 'Approximate -- verify against current edition before publishing'),
 (@u, 'ARWU / Shanghai Ranking (global)', 501, 2025, 'Approximate band (501-600) -- verify');

-- ============================================================================
-- 11. Henan University of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Henan University of Technology', '河南工业大学',
   'henan-university-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Zhengzhou', 'Henan',
   1956, 33500, 600, 2100, 'https://www.haut.edu.cn/', 'Provincial key',
   'Henan University of Technology (HAUT) is a provincial key public university in Zhengzhou, Henan, nationally recognised as a leading institution for grain, oils and food engineering -- reflecting Henan''s role as one of China''s major grain-producing provinces. Beyond its food and grain-engineering speciality, it also teaches mechanical, electrical, chemical, materials, economics and management fields. It is not a Project 211 or 985 institution but holds a strong national reputation in its core specialist disciplines.',
   'The university''s origins lie in the Zhengzhou Institute of Technology and the Zhengzhou Industrial Higher Vocational School, both established in 1956 to support China''s grain-storage and food-processing industries. The two institutions were merged and reorganised over subsequent decades, and the university adopted its present name, Henan University of Technology, in 2004, reflecting its growth into a broader engineering and technology university while retaining its founding strength in grain and food engineering.',
   'The main campus is located in the High-Tech Zone of Zhengzhou, Henan''s provincial capital, with dedicated laboratories for grain storage, food processing and oil-engineering research, alongside general engineering and management facilities. Student accommodation is provided in on-campus dormitories, and Zhengzhou''s status as a major national rail and logistics hub gives students strong transport connectivity.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Ranking position varies slightly between sources (approximately #208-210 in the 2026 BCUR table) -- verify exact current position before publishing. International-student count and faculty count are approximate -- verify. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Henan University of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial key university and China''s leading institution for grain, oils and food engineering'),
 (@u, 'HIGHLIGHT', 2,  'Formed in 2004 from the merger of two 1956-founded technical institutes'),
 (@u, 'HIGHLIGHT', 3,  'Strong mechanical, electrical, chemical and materials-engineering programmes'),
 (@u, 'HIGHLIGHT', 4,  'Located in the High-Tech Zone of Zhengzhou, Henan''s provincial capital'),
 (@u, 'HIGHLIGHT', 5,  'Dedicated grain-storage, food-processing and oil-engineering laboratories'),
 (@u, 'HIGHLIGHT', 6,  'Large enrolment of over 33,000 undergraduate and postgraduate students'),
 (@u, 'HIGHLIGHT', 7,  'Deep ties to Henan''s grain-producing agricultural economy'),
 (@u, 'HIGHLIGHT', 8,  'Broad coverage of economics and management alongside engineering'),
 (@u, 'HIGHLIGHT', 9,  'Zhengzhou is one of China''s major national rail and logistics hubs'),
 (@u, 'HIGHLIGHT', 10, 'Recognised national specialist status in food and grain engineering despite non-211/985 status'),
 (@u, 'ADVANTAGE', 11, 'National-level specialisation in food and grain engineering, a growth sector'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting food science, agricultural engineering or logistics careers'),
 (@u, 'ADVANTAGE', 13, 'Zhengzhou''s central location gives excellent high-speed rail connectivity across China'),
 (@u, 'ADVANTAGE', 14, 'Lower cost of living than China''s coastal provincial capitals'),
 (@u, 'ADVANTAGE', 15, 'Strong regional employer network in food processing, logistics and manufacturing'),
 (@u, 'ADVANTAGE', 16, 'Established international-student intake channel'),
 (@u, 'ADVANTAGE', 17, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 18, 'Applied, industry-linked curriculum in its specialist food-engineering disciplines'),
 (@u, 'ADVANTAGE', 19, 'Zhengzhou''s growing logistics and e-commerce sector offers internship opportunities'),
 (@u, 'ADVANTAGE', 20, 'Modern High-Tech Zone campus with recent facility investment');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ShanghaiRanking BCUR', 208, 2026, 'Sources give #208-210 depending on edition/snapshot -- verify against current edition before publishing');

-- ============================================================================
-- 12. Hubei University of Arts and Science
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hubei University of Arts and Science', '湖北文理学院',
   'hubei-university-of-arts-and-science', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Xiangyang', 'Hubei',
   1958, 21000, NULL, 1219, 'https://www.hbuas.edu.cn/', 'Provincial undergrad',
   'Hubei University of Arts and Science is a provincial public university in Xiangyang, Hubei, offering a broad range of engineering, science, liberal-arts, medicine and management programmes across more than sixty undergraduate majors. Despite its English name, it is a comprehensive multi-disciplinary university rather than a specialist arts college, with a particular emphasis on engineering and transportation-related fields alongside its founding teacher-education roots. It is not a Project 211 or 985 institution and serves primarily as a regional undergraduate university for northwest Hubei.',
   'The university''s roots trace to Xiangyang Normal College, founded in May 1958. In 1998 it merged with the Xiangyang Normal Senior Vocational School, the Xiangfan Vocational University and the Xiangfan Education College to form Xiangfan College. Following the 2010 renaming of Xiangfan city to Xiangyang, the university was itself renamed Hubei University of Arts and Science in 2012, reflecting both the city''s renaming and the institution''s growth into a broader multi-disciplinary university.',
   'The main campus is located on Longzhong Road in Xiangyang, Hubei, near the historic Longzhong scenic area associated with the Three Kingdoms period. Facilities support engineering, science, medicine, liberal-arts and management teaching, with a staff of over 1,700 including more than 1,200 full-time teachers. Student accommodation is provided in on-campus dormitories, and the campus location gives students access to Xiangyang''s historical and cultural sites.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Enrolment figure is approximate -- verify before publishing; international-student count not found in public sources and left NULL pending verification. No major-table ranking found for this institution; add manually if one appears in a later edition. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Hubei University of Arts and Science' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public university with over sixty undergraduate majors'),
 (@u, 'HIGHLIGHT', 2,  'Comprehensive multi-disciplinary profile despite its "Arts and Science" name'),
 (@u, 'HIGHLIGHT', 3,  'Roots trace to Xiangyang Normal College, founded in 1958'),
 (@u, 'HIGHLIGHT', 4,  'Renamed Hubei University of Arts and Science in 2012 following Xiangyang''s city renaming'),
 (@u, 'HIGHLIGHT', 5,  'Located near the historic Longzhong scenic area of Three Kingdoms heritage'),
 (@u, 'HIGHLIGHT', 6,  'Engineering, science, medicine, liberal-arts and management programmes'),
 (@u, 'HIGHLIGHT', 7,  'Master''s degree authorisation across several disciplines'),
 (@u, 'HIGHLIGHT', 8,  'Full-time student body of over 21,000'),
 (@u, 'HIGHLIGHT', 9,  'Faculty of over 1,200 full-time teachers'),
 (@u, 'HIGHLIGHT', 10, 'Serves as the main public undergraduate university for the Xiangyang region'),
 (@u, 'ADVANTAGE', 11, 'Lower cost of living than provincial-capital universities'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants seeking a broad, comprehensive undergraduate curriculum'),
 (@u, 'ADVANTAGE', 13, 'Access to Xiangyang''s Three Kingdoms-era historical and cultural sites'),
 (@u, 'ADVANTAGE', 14, 'Regional employer network across Xiangyang''s manufacturing and automotive industries'),
 (@u, 'ADVANTAGE', 15, 'Smaller class sizes than larger comprehensive Hubei universities'),
 (@u, 'ADVANTAGE', 16, 'Growing openness to international-student recruitment'),
 (@u, 'ADVANTAGE', 17, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 18, 'Supportive environment for Chinese-language study alongside a degree'),
 (@u, 'ADVANTAGE', 19, 'Rail links connecting Xiangyang to Wuhan and other Hubei cities'),
 (@u, 'ADVANTAGE', 20, 'Responsive to partner-managed recruitment given its regional focus');
-- No ranking rows: institution is not present in major ranking tables. Add in dashboard if one appears.
-- ============================================================================
-- 13. Hunan University of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hunan University of Technology', '湖南工业大学',
   'hunan-university-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Zhuzhou', 'Hunan',
   1979, 31000, NULL, NULL, 'https://www.hut.edu.cn/', 'Provincial undergrad',
   'Hunan University of Technology is a provincial public university in Zhuzhou, Hunan, best known nationally for its packaging engineering and printing/graphic-arts programs, alongside a broad range of engineering, business and design disciplines. It holds masters-degree authorization in multiple fields and serves a large student body drawn mainly from Hunan and neighbouring provinces. The university has an active international-cooperation office and has taken in international students in recent years, though its international profile is still modest compared with Hunan''s 211/Double First-Class institutions.',
   'The university traces its roots to 1979 and went through several mergers and upgrades before taking its current name and university status. It built its reputation around packaging engineering, an unusually specialised niche among Chinese public universities, supported by dedicated national and provincial research platforms. Over the following decades it broadened into a multi-disciplinary institution covering engineering, science, business, design and humanities.',
   'The main campus is located in Zhuzhou, an industrial city in Hunan roughly 40 minutes by high-speed rail from Changsha. The campus houses provincial and national key laboratories tied to packaging and printing technology, along with standard teaching, library and sports facilities typical of a mid-sized Chinese public university.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Total-students figure (~31,000) is from a ~January 2020 count and should be refreshed against current official data before publishing. Faculty count and international-student headcount could not be verified and are left NULL -- confirm before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Hunan University of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Ranked 271st nationally in the 2024 Shanghai Ranking (Ruanke) China University Rankings'),
 (@u, 'HIGHLIGHT', 2,  'Nationally recognised strength in packaging engineering, a rare specialised discipline among Chinese public universities'),
 (@u, 'HIGHLIGHT', 3,  'Founded in 1979 and upgraded to full university status through successive mergers'),
 (@u, 'HIGHLIGHT', 4,  'Located in Zhuzhou, an industrial city in Hunan province with strong manufacturing ties'),
 (@u, 'HIGHLIGHT', 5,  'Offers masters-degree programs across multiple engineering and science disciplines'),
 (@u, 'HIGHLIGHT', 6,  'Home to provincial and national research platforms in printing and packaging technology'),
 (@u, 'HIGHLIGHT', 7,  'Broad multi-disciplinary offering spanning engineering, business, design and humanities'),
 (@u, 'HIGHLIGHT', 8,  'Approximately 30,000+ students as of the most recent public count (2020)'),
 (@u, 'HIGHLIGHT', 9,  'Roughly 40 minutes from Changsha by high-speed rail'),
 (@u, 'HIGHLIGHT', 10, 'Established international-cooperation office with growing partner network'),
 (@u, 'ADVANTAGE', 11, 'A niche, less internationally saturated engineering brand for students targeting packaging, printing or materials careers'),
 (@u, 'ADVANTAGE', 12, 'Lower cost of living in Zhuzhou compared with tier-1 Chinese cities'),
 (@u, 'ADVANTAGE', 13, 'Fast rail access to Changsha for internships, transport links and city amenities'),
 (@u, 'ADVANTAGE', 14, 'Practical, industry-linked engineering training suited to students planning to work in manufacturing or packaging sectors'),
 (@u, 'ADVANTAGE', 15, 'A realistic admissions target for applicants who may not meet the bar at Hunan''s 211/Double First-Class universities'),
 (@u, 'ADVANTAGE', 16, 'Growing but still small international cohort, useful for students who want more individual attention from international staff'),
 (@u, 'ADVANTAGE', 17, 'Postgraduate pathway available on campus for students who want to continue directly into a masters program'),
 (@u, 'ADVANTAGE', 18, 'Business and design programs alongside engineering, useful for students wanting a broader course choice within one campus'),
 (@u, 'ADVANTAGE', 19, 'Provincial capital resources (Changsha) reachable for weekend travel, job fairs and larger student communities'),
 (@u, 'ADVANTAGE', 20, 'Established institution (46+ years) with a stable administrative and academic structure');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'Shanghai Ranking (Ruanke)', 271, 2024, 'Verified via shanghairanking.cn institution page');

-- ============================================================================
-- 14. Huzhou University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Huzhou University', '湖州学院',
   'huzhou-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Huzhou', 'Zhejiang',
   1999, 8600, NULL, NULL, 'http://www.zjhzu.edu.cn/', 'Public (converted 2021)',
   'Huzhou University is a small public undergraduate university in Huzhou, Zhejiang, that only became an independent public institution in 2021. It offers a modest range of undergraduate programs and is at an early stage of building an international profile, so applicants should expect a smaller campus community and fewer established international-student services than at longer-established universities in the catalog.',
   'The institution began in 1999 as Qiuzhen College, an independent college affiliated with the older Huzhou Normal University. In January 2021 the Ministry of Education approved its conversion from an independent (formerly private-run) college into a directly public, independently established undergraduate university under the name Huzhou University.',
   'The campus sits in Huzhou, a mid-sized Zhejiang city on the southern shore of Lake Tai, within reasonable travel distance of Hangzhou and Shanghai. As a recently converted institution the campus and facilities are still being built out to match its new independent-university status.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. NAME AMBIGUITY -- source brief said "formerly Huzhou Teachers College," but that lineage belongs to a DIFFERENT institution, Huzhou Normal University (huznu.edu.cn). This record is 湖州学院 (Huzhou College, ex-Qiuzhen College, converted 2021) -- a distinct, smaller school. Staff must confirm which institution was intended; website/enrollment unverified, secondary-source only. No credible ranking found. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Huzhou University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Became an independently established public undergraduate university in January 2021'),
 (@u, 'HIGHLIGHT', 2,  'Predecessor institution (Qiuzhen College) founded in 1999'),
 (@u, 'HIGHLIGHT', 3,  'Located in Huzhou, on the southern shore of Lake Tai in northern Zhejiang'),
 (@u, 'HIGHLIGHT', 4,  'Small, still-growing campus community of roughly 8,600 students'),
 (@u, 'HIGHLIGHT', 5,  'Offers 35 undergraduate programs across multiple disciplines'),
 (@u, 'HIGHLIGHT', 6,  'Campus occupies over 1,000 acres with roughly 363,600 sq m of building area'),
 (@u, 'HIGHLIGHT', 7,  'Within commuting/travel distance of Hangzhou and Shanghai'),
 (@u, 'HIGHLIGHT', 8,  'One of a recent wave of independent colleges converted to public university status nationally'),
 (@u, 'HIGHLIGHT', 9,  'Zhejiang provincial oversight following its 2021 public conversion'),
 (@u, 'HIGHLIGHT', 10, 'Historically linked to Huzhou Normal University, a longer-established neighbouring institution'),
 (@u, 'ADVANTAGE', 11, 'Smaller student body can mean more individual attention from faculty and administration'),
 (@u, 'ADVANTAGE', 12, 'Lake Tai region location offers a lower cost of living than Hangzhou or Shanghai while remaining close to both'),
 (@u, 'ADVANTAGE', 13, 'A newer public university may be more open to building fresh international partnerships and pilot programs'),
 (@u, 'ADVANTAGE', 14, 'Zhejiang provincial economy offers strong internship and employment opportunities in manufacturing and e-commerce'),
 (@u, 'ADVANTAGE', 15, 'Public-university status since 2021 provides more institutional stability than its former independent-college status'),
 (@u, 'ADVANTAGE', 16, 'Proximity to Shanghai and Hangzhou for weekend travel, cultural access and larger international communities'),
 (@u, 'ADVANTAGE', 17, 'Modest admissions bar relative to Zhejiang''s more prominent public universities'),
 (@u, 'ADVANTAGE', 18, 'Growing campus infrastructure as the university invests in its new independent status'),
 (@u, 'ADVANTAGE', 19, 'Broad general undergraduate offering suitable for students still deciding on a specialisation'),
 (@u, 'ADVANTAGE', 20, 'Regional Zhejiang setting known for a mild climate and scenic Lake Tai surroundings');
-- No ranking rows: institution converted to public status in 2021 and is not present in major national ranking tables. Add in dashboard if one appears.

-- ============================================================================
-- 15. Jiangsu Vocational College of Medicine
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Jiangsu Vocational College of Medicine', '江苏医药职业学院',
   'jiangsu-vocational-college-of-medicine', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Yancheng', 'Jiangsu',
   1941, 10000, NULL, 629, 'https://eng.jsmc.edu.cn/', 'Vocational college',
   'Jiangsu Vocational College of Medicine is a public higher-vocational (associate-degree, "higher zhuanke") institution in Yancheng, Jiangsu, directly overseen by the Jiangsu Provincial Health Commission. It is NOT a bachelor''s-degree-granting university -- it offers three-year diploma/associate-degree programs in nursing, pharmacy, medical technology and related allied-health fields, not undergraduate degrees. It has an international-cooperation footprint, including a branch college inaugurated in Malta in December 2024.',
   'The college traces its institutional lineage to a wartime sanitation school established in 1941 and has been reorganised and renamed several times since, most recently taking its current form and name as a provincially administered vocational college of medicine. The precise chain of mergers between 1941 and the present institution could not be fully verified from public sources.',
   'The campus in Yancheng covers approximately 562,000 square meters (about 843 mu) and comprises 12 teaching units offering 25 higher-vocational majors, with facilities oriented toward clinical, nursing and pharmaceutical skills training rather than research infrastructure.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: STAFF DECISION REQUIRED -- this is a diploma-level vocational college (associate-degree, 高职高专), not a bachelor''''s university; staff must decide if it belongs in this catalog before publishing. Official English name confirmed as "Jiangsu Vocational College of Medicine" (source brief said "Jiangsu College of Medicine"). Founded-year reflects earliest predecessor only. Enrollment approximate. No bachelor-level ranking invented. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Jiangsu Vocational College of Medicine' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Public higher-vocational college directly administered by the Jiangsu Provincial Health Commission'),
 (@u, 'HIGHLIGHT', 2,  'Institutional lineage traces to a wartime sanitation school established in 1941'),
 (@u, 'HIGHLIGHT', 3,  'Offers 25 higher-vocational (associate-degree) majors across 12 teaching units'),
 (@u, 'HIGHLIGHT', 4,  'Five backbone majors recognised under the Ministry of Education''s Innovation Development Action Plan'),
 (@u, 'HIGHLIGHT', 5,  'Campus covers approximately 562,000 square meters in Yancheng, Jiangsu'),
 (@u, 'HIGHLIGHT', 6,  '629 faculty and staff members as of April 2025'),
 (@u, 'HIGHLIGHT', 7,  'Opened an overseas branch college in Malta in December 2024'),
 (@u, 'HIGHLIGHT', 8,  'Focus areas include nursing, pharmacy and medical-technology diploma training'),
 (@u, 'HIGHLIGHT', 9,  'One provincial high-level major cluster recognised by Jiangsu authorities'),
 (@u, 'HIGHLIGHT', 10, 'Located in Yancheng, a coastal prefecture-level city in Jiangsu province'),
 (@u, 'ADVANTAGE', 11, 'Direct, practical route into allied-health careers without the length or cost of a full bachelor''s program'),
 (@u, 'ADVANTAGE', 12, 'Provincial health-system administration gives graduates clear links to Jiangsu hospitals and clinics'),
 (@u, 'ADVANTAGE', 13, 'Existing Malta branch campus signals real (if early-stage) international program infrastructure'),
 (@u, 'ADVANTAGE', 14, 'Lower tuition and cost of living in Yancheng compared with Nanjing or Suzhou'),
 (@u, 'ADVANTAGE', 15, 'Shorter, three-year program length suits students who want to enter the workforce quickly'),
 (@u, 'ADVANTAGE', 16, 'Hands-on clinical and pharmaceutical skills training rather than a purely academic curriculum'),
 (@u, 'ADVANTAGE', 17, 'Accessible admissions bar relative to Jiangsu''s bachelor-degree medical universities'),
 (@u, 'ADVANTAGE', 18, 'Potential articulation/upgrade routes into bachelor-level nursing or health programs for strong graduates (verify with the college)'),
 (@u, 'ADVANTAGE', 19, 'Established, decades-old institution with a stable provincial-government backing'),
 (@u, 'ADVANTAGE', 20, 'Growing international outlook, useful for students interested in an eventual cross-border health career');
-- No ranking rows: institution is a higher-vocational (associate-degree) college and is not present in bachelor-level ranking tables. Do not add a bachelor-degree ranking for this entry.

-- ============================================================================
-- 16. Jiangxi University of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Jiangxi University of Technology', '江西科技学院',
   'jiangxi-university-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PRIVATE', 'Nanchang', 'Jiangxi',
   1994, 38000, NULL, NULL, 'https://www.jxut.edu.cn/', 'Private undergrad',
   'Jiangxi University of Technology is a large private nonprofit undergraduate university in Nanchang, Jiangxi, offering programs across engineering, business, computing and applied disciplines with a strong emphasis on practical, vocationally oriented training. It is one of Jiangxi''s largest private higher-education institutions by enrollment and has repeatedly been cited as a leading private university within the province and nationally.',
   'The university was founded in 1994 and operated for years as Jiangxi Blue Sky University before being renamed Jiangxi University of Technology in 2012. Since then it has expanded into a large, multi-faculty private university with an extensive network of practical training centers covering fields such as vehicle technology, mechanical engineering, computing and modern production technology.',
   'The main campus is in Nanchang, the Jiangxi provincial capital, and includes 17 dedicated practical-training centers such as a vehicle-technology center, mechanical-engineering laboratory, computer-technology laboratory and modern-production-technology center, reflecting the university''s applied, employment-oriented teaching model.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: NAME IS A BEST GUESS, NOT CONFIRMED -- source brief said only "Jiangxi Institutes of Technology" (garbled), matching no real institution exactly. Identified as best match: 江西科技学院 / Jiangxi University of Technology, private university in Nanchang (founded 1994, renamed 2012). MUST be confirmed by staff before publishing. Claimed #1-private-university ranking is from an unverified source -- treat as approximate. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Jiangxi University of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'One of the largest private universities in Jiangxi province by enrollment (approximately 38,000 undergraduates)'),
 (@u, 'HIGHLIGHT', 2,  'Founded in 1994 as Jiangxi Blue Sky University; renamed Jiangxi University of Technology in 2012'),
 (@u, 'HIGHLIGHT', 3,  'Reported as a top-ranked private university in China by at least one third-party ranking body since 2007 (unverified methodology)'),
 (@u, 'HIGHLIGHT', 4,  '17 dedicated practical-training centers covering vehicle technology, mechanical engineering, computing and production technology'),
 (@u, 'HIGHLIGHT', 5,  'Located in Nanchang, the capital and largest city of Jiangxi province'),
 (@u, 'HIGHLIGHT', 6,  'Broad program offering spanning engineering, business, computing and applied sciences'),
 (@u, 'HIGHLIGHT', 7,  'Applied, employment-oriented teaching model emphasising hands-on training centers'),
 (@u, 'HIGHLIGHT', 8,  'Private nonprofit status with over three decades of continuous operation'),
 (@u, 'HIGHLIGHT', 9,  'Active social-media and international-outreach presence (e.g. official Facebook page)'),
 (@u, 'HIGHLIGHT', 10, 'Large enough scale to support a wide elective and specialisation choice within one campus'),
 (@u, 'ADVANTAGE', 11, 'Strong applied/vocational orientation suited to students who want job-ready technical skills'),
 (@u, 'ADVANTAGE', 12, 'Large scale gives a wide choice of majors and student communities within a single campus'),
 (@u, 'ADVANTAGE', 13, 'Nanchang location offers a lower cost of living than China''s first-tier cities while still being a provincial capital'),
 (@u, 'ADVANTAGE', 14, 'Private-university admissions flexibility can suit students with less conventional academic backgrounds'),
 (@u, 'ADVANTAGE', 15, 'Extensive practical-training infrastructure supports direct-to-industry career preparation'),
 (@u, 'ADVANTAGE', 16, 'Three decades of institutional track record reduces the risk profile typical of newer private colleges'),
 (@u, 'ADVANTAGE', 17, 'Nanchang''s growing manufacturing and technology sectors offer local internship and employment options'),
 (@u, 'ADVANTAGE', 18, 'Established international-outreach channels for prospective overseas applicants'),
 (@u, 'ADVANTAGE', 19, 'Broad engineering and business curriculum allows students to pivot specialisation after enrollment'),
 (@u, 'ADVANTAGE', 20, 'Sizeable student body supports active campus life, clubs and student services');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'NSEE China (self-reported)', 1, 2024, 'Institution-reported claim of #1 among Chinese private universities since 2007 -- methodology and current standing not independently verified; verify before publishing');

-- ============================================================================
-- 17. Lanzhou University of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Lanzhou University of Technology', '兰州理工大学',
   'lanzhou-university-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Lanzhou', 'Gansu',
   1919, 27600, NULL, NULL, 'http://www.lut.edu.cn/', 'Provincial engineering',
   'Lanzhou University of Technology is a public engineering-focused university in Lanzhou, the capital of Gansu province in northwest China. It is one of Gansu''s leading engineering universities and was named among China''s top 100 universities for engineering strength in a December 2024 national evaluation, with particular recognition in materials science, mechanical engineering and process equipment.',
   'The university''s roots trace to a technical school established in 1919. It grew through the 20th century into a comprehensive engineering-oriented university and, following provincial-national co-construction arrangements common to Chinese regional universities, now offers undergraduate through doctoral programs across engineering, science, management and other fields.',
   'The main campus is located in Lanzhou, a major transport and industrial hub in northwest China along the upper Yellow River. As a long-established engineering university, it maintains laboratories and research platforms tied to materials science and mechanical/process engineering, its traditional areas of strength.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Founded-year (1919) reflects the earliest traceable predecessor technical school; exact merger history to the present university not fully traced -- confirm. Total-students (~27,600) combines separately reported undergrad/postgrad counts -- re-verify against one current source. Faculty and international-student counts not verified, left NULL. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Lanzhou University of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Ranked 186th nationally in the 2024 Shanghai Ranking (Ruanke) China University Rankings'),
 (@u, 'HIGHLIGHT', 2,  'Named among China''s top 100 universities for engineering strength (B+ tier) in a December 2024 national evaluation'),
 (@u, 'HIGHLIGHT', 3,  'Ranked 3rd among universities within Gansu province in the 2024 Shanghai Ranking'),
 (@u, 'HIGHLIGHT', 4,  'Roots trace to a technical school established in 1919'),
 (@u, 'HIGHLIGHT', 5,  'Particular recognition in materials science, mechanical engineering and process equipment'),
 (@u, 'HIGHLIGHT', 6,  'Approximately 23,700 undergraduate and 3,900 postgraduate students'),
 (@u, 'HIGHLIGHT', 7,  'Offers programs from undergraduate through doctoral level'),
 (@u, 'HIGHLIGHT', 8,  'Located in Lanzhou, a major industrial and transport hub in northwest China'),
 (@u, 'HIGHLIGHT', 9,  'Ranked in the 701-800 band globally in the Shanghai Ranking of world universities (ARWU-style)'),
 (@u, 'HIGHLIGHT', 10, 'Long-established (100+ years) engineering-focused public university'),
 (@u, 'ADVANTAGE', 11, 'Strong, recognised engineering brand for students targeting materials science or mechanical engineering'),
 (@u, 'ADVANTAGE', 12, 'Significantly lower cost of living in Lanzhou compared with eastern Chinese cities'),
 (@u, 'ADVANTAGE', 13, 'Gansu''s leading engineering university, offering strong regional employer recognition'),
 (@u, 'ADVANTAGE', 14, 'Postgraduate pathway available on campus for students planning to continue to a masters or doctorate'),
 (@u, 'ADVANTAGE', 15, 'A realistic and achievable admissions target relative to China''s most selective engineering universities'),
 (@u, 'ADVANTAGE', 16, 'Northwest China location offers a distinctive cultural and travel base (Silk Road region access)'),
 (@u, 'ADVANTAGE', 17, 'Well-established research platforms in materials and process engineering for research-minded applicants'),
 (@u, 'ADVANTAGE', 18, 'Over a century of institutional history provides administrative stability and alumni networks'),
 (@u, 'ADVANTAGE', 19, 'Lanzhou''s role as a regional transport hub supports internship access across northwest China'),
 (@u, 'ADVANTAGE', 20, 'Broad engineering curriculum allows specialisation choice after enrollment');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'Shanghai Ranking (Ruanke)', 186, 2024, 'Verified via shanghairanking.cn institution page and official LUT news release');
-- Additional global-band claim (Shanghai Ranking ARWU 701-800 band, 2024) omitted: no single numeric rank_position published for that band.

-- ============================================================================
-- 18. Liupanshui Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Liupanshui Normal University', '六盘水师范学院',
   'liupanshui-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Liupanshui', 'Guizhou',
   1978, 7000, NULL, NULL, 'http://www.lpssy.edu.cn', 'Provincial normal univ.',
   'Liupanshui Normal University is a small public teacher-training (normal) university in Liupanshui, a mountainous prefecture-level city in western Guizhou province. It is a regional institution focused primarily on teacher education alongside a smaller range of other undergraduate programs, and it sits toward the lower end of China''s national university rankings.',
   'The institution originated as a branch campus of Guiyang Normal College established in Liupanshui in 1978. In 1985 the Guizhou provincial government approved its establishment as an independent Liupanshui Normal College, and in March 2009 the Ministry of Education approved its upgrade to full university status as Liupanshui Normal University.',
   'The campus is located in Liupanshui, a mountainous city in western Guizhou known for coal mining and a cooler climate than much of southern China. As a smaller regional normal university, its facilities are oriented primarily toward teacher-training and general undergraduate instruction rather than large-scale research infrastructure.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Founded-year set to 1978 (original branch campus); institution became independent in 1985 and gained university status in 2009 -- confirm which milestone the catalog should treat as "founded." Total-students (~7,000) and faculty/international-student counts from a single secondary source, not cross-verified. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Liupanshui Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Ranked 496th nationally in the 2024 Shanghai Ranking (Ruanke) China University Rankings'),
 (@u, 'HIGHLIGHT', 2,  'Originated as a Guiyang Normal College branch campus established in 1978'),
 (@u, 'HIGHLIGHT', 3,  'Upgraded to full university status in March 2009 by Ministry of Education approval'),
 (@u, 'HIGHLIGHT', 4,  'Focused primarily on teacher-education (normal-university) programs'),
 (@u, 'HIGHLIGHT', 5,  'Located in Liupanshui, a mountainous western Guizhou city'),
 (@u, 'HIGHLIGHT', 6,  'Compact student body of approximately 7,000 full-time students'),
 (@u, 'HIGHLIGHT', 7,  'Regional focus serving western Guizhou''s teacher-training needs'),
 (@u, 'HIGHLIGHT', 8,  'National ranking position has been broadly stable in the 470-530 range over 2020-2024'),
 (@u, 'HIGHLIGHT', 9,  'Public provincial university under Guizhou provincial government oversight'),
 (@u, 'HIGHLIGHT', 10, 'Smaller-scale campus and class sizes typical of a regional normal university'),
 (@u, 'ADVANTAGE', 11, 'Low cost of living in Liupanshui compared with almost any other city in this catalog'),
 (@u, 'ADVANTAGE', 12, 'Small class sizes and a compact campus community can mean closer faculty contact'),
 (@u, 'ADVANTAGE', 13, 'Teacher-education focus suits students specifically targeting an education-sector career'),
 (@u, 'ADVANTAGE', 14, 'An accessible admissions bar relative to Guizhou''s more competitive institutions'),
 (@u, 'ADVANTAGE', 15, 'Cooler, mountainous climate as a distinctive alternative to China''s hotter, more crowded cities'),
 (@u, 'ADVANTAGE', 16, 'Public-university status offers institutional stability despite the university''s smaller size'),
 (@u, 'ADVANTAGE', 17, 'Straightforward path into regional teaching roles across western Guizhou'),
 (@u, 'ADVANTAGE', 18, 'Fewer international applicants at present may mean more individualised support for those who do enroll'),
 (@u, 'ADVANTAGE', 19, 'A genuinely regional China experience away from the more internationalised eastern-coast campuses'),
 (@u, 'ADVANTAGE', 20, 'Stable, decades-old public institution with clear provincial-government backing');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'Shanghai Ranking (Ruanke)', 496, 2024, 'Verified via shanghairanking.cn institution page; prior editions: 477 (2020), 484 (2021), 527 (2022), 526 (2023)');

-- ============================================================================
-- 19. Mianyang Polytechnic
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Mianyang Polytechnic', '绵阳职业技术学院',
   'mianyang-polytechnic', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Mianyang', 'Sichuan',
   2001, 11000, NULL, 784, 'http://www.mypt.edu.cn', 'Vocational college',
   'Mianyang Polytechnic is a public higher-vocational (associate-degree) college in Mianyang, Sichuan, established by the Mianyang municipal government. It is NOT a bachelor''s-degree-granting university -- it offers three-year diploma/associate-degree programs, primarily in engineering-adjacent fields such as materials, mechanical-and-electrical, information and construction engineering. It was one of the first batch of higher-vocational colleges established in Sichuan province and holds recognition as a national model vocational institution.',
   'The college was approved for establishment by the Sichuan provincial government in 2001 and was among the first eight higher-vocational schools set up in the province. Some secondary sources attribute an earlier, unrelated 1933 predecessor (a glass-vocational school in Jiangsu province) to this institution; that lineage claim could not be verified against Mianyang Polytechnic''s own official materials and appears likely to be a conflation with a different school -- it has been omitted from this record pending confirmation.',
   'The campus is in Mianyang, an important technology and defence-research city in Sichuan province, and covers more than 700 acres with roughly 300,000 square meters of school buildings. It is organised into eight departments -- including materials engineering, mechanical-and-electrical engineering, information engineering, computer science, construction engineering, management engineering, humanities and art -- offering 55 higher-vocational majors.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: STAFF DECISION REQUIRED -- diploma-level vocational college (associate-degree, 高职高专), not a bachelor''''s university; staff must decide if it belongs in this catalog. A secondary source''''s unrelated 1933 Jiangsu lineage was discarded (likely data-conflation); founded_year (2001) is the confirmed Sichuan provincial approval date. Enrollment/faculty from one secondary source -- cross-check mypt.edu.cn. No ranking invented. Partner status PROSPECT pending BD; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Mianyang Polytechnic' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'One of the first eight higher-vocational colleges established in Sichuan province (2001)'),
 (@u, 'HIGHLIGHT', 2,  'Recognised as a national model higher-vocational college'),
 (@u, 'HIGHLIGHT', 3,  'Offers 55 higher-vocational majors across eight departments'),
 (@u, 'HIGHLIGHT', 4,  'Established by the Mianyang municipal government'),
 (@u, 'HIGHLIGHT', 5,  'Campus of more than 700 acres with roughly 300,000 sq m of buildings'),
 (@u, 'HIGHLIGHT', 6,  '784 faculty members, including senior and deputy-senior titled staff'),
 (@u, 'HIGHLIGHT', 7,  'Engineering-oriented departments spanning materials, mechanical-electrical, information and construction fields'),
 (@u, 'HIGHLIGHT', 8,  'Located in Mianyang, a significant technology and research city in Sichuan'),
 (@u, 'HIGHLIGHT', 9,  'Over 11,000 enrolled vocational-college students'),
 (@u, 'HIGHLIGHT', 10, 'More than two decades of operation as a provincially approved vocational institution'),
 (@u, 'ADVANTAGE', 11, 'Practical, industry-aligned diploma programs suited to students seeking fast entry into technical careers'),
 (@u, 'ADVANTAGE', 12, 'Mianyang''s strong technology and manufacturing base offers local internship and employment links'),
 (@u, 'ADVANTAGE', 13, 'Lower cost of living than Chengdu while remaining within the same province'),
 (@u, 'ADVANTAGE', 14, 'Shorter three-year program length for students wanting to enter the workforce quickly'),
 (@u, 'ADVANTAGE', 15, 'National-model vocational-college status signals above-average teaching-quality recognition within its tier'),
 (@u, 'ADVANTAGE', 16, 'Wide choice of 55 majors gives flexibility in choosing a technical specialisation'),
 (@u, 'ADVANTAGE', 17, 'Accessible admissions bar relative to Sichuan''s bachelor-degree universities'),
 (@u, 'ADVANTAGE', 18, 'Municipal-government backing provides administrative and funding stability'),
 (@u, 'ADVANTAGE', 19, 'Sichuan''s growing electronics and technology sector offers regional career pathways'),
 (@u, 'ADVANTAGE', 20, 'Established, multi-decade track record as a provincially recognised vocational institution');
-- No ranking rows: institution is a higher-vocational (associate-degree) college and is not present in bachelor-level ranking tables. Do not add a bachelor-degree ranking for this entry.

-- ============================================================================
-- 20. Nanchang Medical College
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Nanchang Medical College', '南昌医学院',
   'nanchang-medical-college', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Nanchang', 'Jiangxi',
   2001, 15000, NULL, 901, 'https://www.ncmc.edu.cn/', 'Public (converted 2021)',
   'Nanchang Medical College is a public full-time undergraduate medical institution in Nanchang, Jiangxi, offering bachelor''s-degree programs across medicine and allied-health fields. It is overseen administratively by the Jiangxi Provincial Health Commission with academic guidance from the Jiangxi Provincial Department of Education, and it operates affiliated teaching hospitals used for clinical training.',
   'The college began in 2001 as a dependent branch campus of Jiangxi University of Chinese Medicine (then Jiangxi College of Traditional Chinese Medicine) in Fuzhou, was renamed its Science and Technology College in 2013, and in January 2021 the Ministry of Education approved its conversion into an independently established institution named Nanchang Medical College. That 2021 conversion also changed its administrative status from a dependent (privately run) affiliated college to a directly public institution.',
   'The college operates across two main campuses, Xianghu and Jiulonghu, covering roughly 970,000 square meters combined. It has clinical ties to five directly affiliated tertiary (Triple-A) hospitals and six non-directly-affiliated hospitals, supporting hands-on clinical training for its medical and nursing programs.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: TYPE CORRECTED FROM BRIEF -- source said PRIVATE, but verified MOE/provincial sources show it became directly PUBLIC upon its January 2021 conversion from a dependent affiliated college; before 2021 it had private-style status. Set to PUBLIC per verified status -- confirm before publishing. A secondary source''''s 1949/1937 founding story belongs to a different institution (Nanchang University''''s Jiangxi Medical College), not used here. Partner status PROSPECT pending BD; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Nanchang Medical College' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Became an independently established public undergraduate medical institution in January 2021'),
 (@u, 'HIGHLIGHT', 2,  'Predecessor branch campus established in 2001 under Jiangxi University of Chinese Medicine'),
 (@u, 'HIGHLIGHT', 3,  'Over 15,000 full-time undergraduate students'),
 (@u, 'HIGHLIGHT', 4,  '901 full-time faculty members'),
 (@u, 'HIGHLIGHT', 5,  'Clinical ties to five directly affiliated Triple-A (tertiary) hospitals'),
 (@u, 'HIGHLIGHT', 6,  'Additional clinical partnerships with six non-directly-affiliated hospitals'),
 (@u, 'HIGHLIGHT', 7,  'Offers 22 undergraduate majors across 12 teaching colleges and departments'),
 (@u, 'HIGHLIGHT', 8,  'Operates two campuses (Xianghu and Jiulonghu) totalling roughly 970,000 sq m'),
 (@u, 'HIGHLIGHT', 9,  'Administered by the Jiangxi Provincial Health Commission'),
 (@u, 'HIGHLIGHT', 10, 'Located in Nanchang, the capital of Jiangxi province'),
 (@u, 'ADVANTAGE', 11, 'Direct hospital affiliations give students real clinical placement opportunities during study'),
 (@u, 'ADVANTAGE', 12, 'Newly public status (since 2021) improves institutional stability and funding relative to its former affiliated-college status'),
 (@u, 'ADVANTAGE', 13, 'Nanchang''s lower cost of living compared with China''s coastal medical hubs'),
 (@u, 'ADVANTAGE', 14, 'Focused medical/allied-health curriculum for students committed to a healthcare career'),
 (@u, 'ADVANTAGE', 15, 'Provincial health-system administration supports clear pathways into Jiangxi''s hospital network'),
 (@u, 'ADVANTAGE', 16, 'Two-campus setup provides distinct spaces for pre-clinical and clinical-phase training'),
 (@u, 'ADVANTAGE', 17, 'Accessible admissions bar relative to China''s top-tier medical universities'),
 (@u, 'ADVANTAGE', 18, 'Recently strengthened public-institution status may bring expanding resources and program development'),
 (@u, 'ADVANTAGE', 19, 'Jiangxi provincial capital location offers broader city amenities alongside a focused medical campus'),
 (@u, 'ADVANTAGE', 20, 'Growing faculty base (901 members) supports a reasonable student-to-faculty ratio for clinical training');
-- No ranking rows: institution is newly independent (converted 2021) and not yet present in major national ranking tables. Add in dashboard if one appears.

-- ============================================================================
-- 21. Ningbo Polytechnic
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Ningbo Polytechnic', '宁波职业技术学院',
   'ningbo-polytechnic', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Ningbo', 'Zhejiang',
   1999, 12000, NULL, NULL, 'https://en.nbpt.edu.cn/', 'Vocational college',
   'Ningbo Polytechnic is a public higher-vocational (associate-degree) college in Ningbo, Zhejiang. It is NOT a bachelor''s-degree-granting university -- it offers three-year diploma/associate-degree programs. It is one of China''s better-regarded vocational colleges, recognised as a national demonstration higher-vocational institution and a national high-level vocational school with distinctive Chinese-characteristic and professional-group-construction status.',
   'The college was formed in 1999 through the merger of predecessor institutions in Ningbo, building on a combined institutional history of more than 60 years. It has since grown into one of the first batch of national demonstration higher-vocational colleges, with recognised strength in teaching management, international engagement and teaching resources.',
   'The campus is in Ningbo, a major port city in Zhejiang province, and supports both full-time vocational students and a larger population of part-time and continuing-education students, reflecting its role as a broad vocational-training hub for the region.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: STAFF DECISION REQUIRED -- diploma-level vocational college (associate-degree, 高职高专), not a bachelor''''s university; staff must decide if it belongs in this catalog. Total-students (12,000) is full-time only; combined with continuing education it may be ~25,000 -- clarify which figure is wanted. No bachelor-level ranking invented; college appears in vocational-only "Top 50" lists but no single rank confirmed. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Ningbo Polytechnic' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'One of the first batch of national demonstration higher-vocational colleges in China'),
 (@u, 'HIGHLIGHT', 2,  'Recognised as a national high-level vocational school with Chinese-characteristic status'),
 (@u, 'HIGHLIGHT', 3,  'Formed in 1999 through the merger of predecessor institutions with 60+ years of combined history'),
 (@u, 'HIGHLIGHT', 4,  'Reported among the "Top 50 Asia-Pacific Vocational Colleges" in 2019'),
 (@u, 'HIGHLIGHT', 5,  'Recognised for teaching management, international influence and teaching resources'),
 (@u, 'HIGHLIGHT', 6,  'Full-time enrollment of over 12,000 vocational-college students'),
 (@u, 'HIGHLIGHT', 7,  'Located in Ningbo, one of China''s largest port cities'),
 (@u, 'HIGHLIGHT', 8,  'Maintains an active English-language website and international-facing social media presence'),
 (@u, 'HIGHLIGHT', 9,  'High-level professional-group-construction unit designation'),
 (@u, 'HIGHLIGHT', 10, 'Broad continuing-education offering alongside its full-time vocational programs'),
 (@u, 'ADVANTAGE', 11, 'Strong, nationally recognised vocational-college reputation among peer institutions'),
 (@u, 'ADVANTAGE', 12, 'Ningbo''s port-city economy offers direct logistics, trade and manufacturing career pathways'),
 (@u, 'ADVANTAGE', 13, 'Established international-engagement track record for a vocational institution'),
 (@u, 'ADVANTAGE', 14, 'Shorter, practical three-year program length for students wanting fast workforce entry'),
 (@u, 'ADVANTAGE', 15, 'Zhejiang''s strong private-sector economy offers abundant internship opportunities'),
 (@u, 'ADVANTAGE', 16, 'Lower admissions bar and cost relative to Zhejiang''s bachelor-degree universities'),
 (@u, 'ADVANTAGE', 17, 'Well-regarded teaching-quality recognition within the national vocational-college tier'),
 (@u, 'ADVANTAGE', 18, 'Ningbo''s coastal location offers good transport links to Shanghai and Hangzhou'),
 (@u, 'ADVANTAGE', 19, 'Long institutional history (60+ years combined) provides administrative stability'),
 (@u, 'ADVANTAGE', 20, 'Broad continuing-education infrastructure supports flexible further study after graduation');
-- No ranking rows: institution is a higher-vocational (associate-degree) college and is not present in bachelor-level ranking tables. A specific numeric vocational-college rank could not be confirmed; add in dashboard if one appears.

-- ============================================================================
-- 22. Ningbo University of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Ningbo University of Technology', '宁波工程学院',
   'ningbo-university-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Ningbo', 'Zhejiang',
   1983, 13470, NULL, NULL, 'https://english.nbut.edu.cn/', 'Provincial engineering',
   'Ningbo University of Technology is a full-time public undergraduate university in Ningbo, Zhejiang, founded and run by the Ningbo municipal government. It is a comprehensive, applied-engineering-oriented institution offering bachelor''s-degree programs across engineering, science, management and other fields.',
   'The university was established in 1983 as Ningbo College and has since developed into a comprehensive municipal public university under its current name, with a continuing focus on applied engineering education aligned with Ningbo''s manufacturing and industrial economy.',
   'The campus is located in Ningbo, a major port and manufacturing city in Zhejiang province, and supports a full-time undergraduate population of roughly 13,500 students across its engineering and applied-science programs.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: NAME CLARIFICATION -- source brief suggested "Ningbo Tech University," which risks confusion with a different institution, NingboTech University (浙大宁波理工学院, Zhejiang University Ningbo Institute of Technology). This record is for 宁波工程学院, whose own official English site (english.nbut.edu.cn) uses "Ningbo University of Technology" -- used that verified name instead. Staff should confirm which institution was actually intended. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Ningbo University of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Ranked around 292nd-293rd nationally in the 2024 Shanghai Ranking (Ruanke) China University Rankings'),
 (@u, 'HIGHLIGHT', 2,  'Founded in 1983 as Ningbo College, run by the Ningbo municipal government'),
 (@u, 'HIGHLIGHT', 3,  'Comprehensive applied-engineering focus aligned with Ningbo''s manufacturing economy'),
 (@u, 'HIGHLIGHT', 4,  'Full-time undergraduate enrollment of approximately 13,470 students'),
 (@u, 'HIGHLIGHT', 5,  'Located in Ningbo, one of China''s largest port and industrial cities'),
 (@u, 'HIGHLIGHT', 6,  'Municipal (city-government-run) public university status'),
 (@u, 'HIGHLIGHT', 7,  'Offers bachelor''s-degree programs across engineering, science and management'),
 (@u, 'HIGHLIGHT', 8,  'Distinct from the separate, ZJU-affiliated "NingboTech University"'),
 (@u, 'HIGHLIGHT', 9,  'Four decades of continuous operation and development'),
 (@u, 'HIGHLIGHT', 10, 'Active English-language institutional website for international audiences'),
 (@u, 'ADVANTAGE', 11, 'Direct alignment with Ningbo''s strong manufacturing and export economy for internships and jobs'),
 (@u, 'ADVANTAGE', 12, 'Municipal-government funding provides steady institutional investment'),
 (@u, 'ADVANTAGE', 13, 'Coastal Zhejiang location with strong transport links to Shanghai and Hangzhou'),
 (@u, 'ADVANTAGE', 14, 'Applied-engineering curriculum suited to students wanting industry-ready technical skills'),
 (@u, 'ADVANTAGE', 15, 'Mid-sized student body balances program variety with manageable class sizes'),
 (@u, 'ADVANTAGE', 16, 'Accessible admissions bar relative to Zhejiang''s most selective public universities'),
 (@u, 'ADVANTAGE', 17, 'Ningbo''s cost of living is generally lower than Shanghai or Hangzhou'),
 (@u, 'ADVANTAGE', 18, 'Established, four-decade institutional history supports administrative stability'),
 (@u, 'ADVANTAGE', 19, 'Port-city location offers logistics, trade and international-business career exposure'),
 (@u, 'ADVANTAGE', 20, 'English-facing institutional materials support a more accessible application process for international students');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'Shanghai Ranking (Ruanke)', 292, 2024, 'Verified via shanghairanking.cn institution page; also reported as 293 in some 2024 summary sources -- confirm exact figure before publishing');

-- ============================================================================
-- 23. Ningbo University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Ningbo University', '宁波大学',
   'ningbo-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Ningbo', 'Zhejiang',
   1986, 25000, NULL, 2300, 'https://www.nbu.edu.cn/', 'Double First-Class',
   'Ningbo University is a comprehensive municipal public university in Ningbo, Zhejiang, and the most prominent university in this batch by national standing. It holds Double First-Class discipline status (selected in the second round in February 2021) and is comparatively well-ranked both nationally and internationally, making it a genuinely competitive option for international applicants.',
   'The university was founded in 1986 with a major donation from Hong Kong entrepreneur Yue-Kong Pao. It has grown into a comprehensive research university covering a broad range of disciplines and was selected for China''s Double First-Class Construction program in its second round (announced February 2021), reflecting recognised strength in at least one designated discipline area.',
   'The main campus is located in Jiangbei district, Ningbo, a major port city in Zhejiang province. As a Double First-Class university, it maintains more extensive research infrastructure than most other institutions in this batch, supporting both undergraduate and postgraduate education across a wide disciplinary range.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Most nationally prominent institution in this batch (Double First-Class, ARWU national rank ~78) -- review for is_recommended/is_featured once partnership status is established. Faculty (~2,300) and total-students (~25,000) are approximate secondary-source figures -- confirm against the official site. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Ningbo University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Double First-Class Construction university (selected second round, February 2021)'),
 (@u, 'HIGHLIGHT', 2,  'Ranked in the 401-500 global band and 78th nationally in the 2024 Academic Ranking of World Universities'),
 (@u, 'HIGHLIGHT', 3,  'Ranked in the 1001-1200 band in the QS World University Rankings 2027 edition'),
 (@u, 'HIGHLIGHT', 4,  'Founded in 1986 with a major donation from Hong Kong entrepreneur Yue-Kong Pao'),
 (@u, 'HIGHLIGHT', 5,  'Approximately 25,000 students and 2,300 academic staff'),
 (@u, 'HIGHLIGHT', 6,  'Comprehensive research-university disciplinary coverage'),
 (@u, 'HIGHLIGHT', 7,  'Located in Jiangbei district, Ningbo, a major Zhejiang port city'),
 (@u, 'HIGHLIGHT', 8,  'Municipal public university with strong Ningbo city-government backing'),
 (@u, 'HIGHLIGHT', 9,  'The most nationally prominent institution among this batch of twelve universities'),
 (@u, 'HIGHLIGHT', 10, 'Nearly four decades of development since its 1986 founding'),
 (@u, 'ADVANTAGE', 11, 'Double First-Class status gives graduates stronger recognition with Chinese employers and graduate programs'),
 (@u, 'ADVANTAGE', 12, 'Comparatively strong international rankings (ARWU, QS) support competitive university applications abroad'),
 (@u, 'ADVANTAGE', 13, 'Ningbo''s port-city economy provides strong internship and employment access'),
 (@u, 'ADVANTAGE', 14, 'Comprehensive disciplinary range allows students to choose from a wide variety of majors'),
 (@u, 'ADVANTAGE', 15, 'Larger research infrastructure than most peer institutions in this batch, useful for research-track students'),
 (@u, 'ADVANTAGE', 16, 'Strong transport links to Shanghai and Hangzhou for travel, internships and networking'),
 (@u, 'ADVANTAGE', 17, 'Well-established international-student services relative to smaller regional universities'),
 (@u, 'ADVANTAGE', 18, 'Sizeable, diverse student community supports a rich campus and cultural life'),
 (@u, 'ADVANTAGE', 19, 'Reasonable cost of living for a Double First-Class university, compared with Beijing or Shanghai equivalents'),
 (@u, 'ADVANTAGE', 20, 'Municipal government investment supports continued facility and program development');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ARWU / Shanghai Ranking', 78, 2024, 'National rank; global band reported as 401-500 -- verify against current edition before publishing');
-- Additional QS World University Rankings claim (band 1001-1200, 2027 edition) omitted: no single numeric rank_position published for that band.

-- ============================================================================
-- 24. Ningbo University of Finance and Economics
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Ningbo University of Finance and Economics', '宁波财经学院',
   'ningbo-university-of-finance-and-economics', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PRIVATE', 'Ningbo', 'Zhejiang',
   2001, 20000, NULL, NULL, 'https://www.nbufe.edu.cn/en/', 'Private finance & econ.',
   'Ningbo University of Finance and Economics is a private (nonprofit) application-oriented undergraduate university in Ningbo, Zhejiang, specialising in economics and management while also offering engineering, humanities and arts programs. It is run on a nonprofit basis by a Ningbo municipal state-owned enterprise, an arrangement that gives it more public-sector-style stability than a typical private Chinese university, though it remains formally classified as a private (民办) institution by China''s Ministry of Education.',
   'The university was founded in 2001, became a full undergraduate institution in 2008, and was renamed from Ningbo Dahongying University to Ningbo University of Finance and Economics in 2018. Throughout its history it has been organised as a nonprofit, publicly beneficial private institution rather than a for-profit private college.',
   'The university operates across three campuses -- Haishu, Hangzhou Bay and Xiangshan -- covering a combined area of over 1,700 acres, and offers 45 undergraduate programs with a primary focus on economics and management alongside engineering, humanities and the arts.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: TYPE CORRECTED FROM BRIEF -- source said PUBLIC, but MOE classification and Chinese-language sources confirm it is formally PRIVATE (民办), a nonprofit run by a Ningbo municipal SOE rather than direct government operation. Set to PRIVATE per verified classification -- confirm before publishing; the nonprofit SOE-run structure is unusual, worth surfacing to BD. Ranks 1st nationally among private finance colleges in a category list, not the general ranking. No MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Ningbo University of Finance and Economics' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Ranked 1st nationally among private finance-and-economics colleges in the Shanghai Ranking (Ruanke) category list'),
 (@u, 'HIGHLIGHT', 2,  'Founded in 2001; became a full undergraduate institution in 2008'),
 (@u, 'HIGHLIGHT', 3,  'Renamed from Ningbo Dahongying University to its current name in 2018'),
 (@u, 'HIGHLIGHT', 4,  'Nonprofit private institution run by a Ningbo municipal state-owned enterprise'),
 (@u, 'HIGHLIGHT', 5,  'Offers 45 undergraduate programs across economics, management, engineering, humanities and arts'),
 (@u, 'HIGHLIGHT', 6,  'Three campuses -- Haishu, Hangzhou Bay and Xiangshan -- totalling over 1,700 acres'),
 (@u, 'HIGHLIGHT', 7,  'Nearly 20,000 full-time undergraduate students'),
 (@u, 'HIGHLIGHT', 8,  'Notable strength in accounting, cited as its flagship program in public commentary'),
 (@u, 'HIGHLIGHT', 9,  'Includes several national first-class ("golden") undergraduate programs and courses'),
 (@u, 'HIGHLIGHT', 10, 'Located in Ningbo, a major Zhejiang port and economic city'),
 (@u, 'ADVANTAGE', 11, 'National #1 standing among private finance-and-economics colleges strengthens its business-program credibility'),
 (@u, 'ADVANTAGE', 12, 'Nonprofit, SOE-backed structure offers more institutional stability than a typical private college'),
 (@u, 'ADVANTAGE', 13, 'Focused economics-and-management curriculum suited to students targeting finance or business careers'),
 (@u, 'ADVANTAGE', 14, 'Three-campus setup provides varied settings, including a coastal Xiangshan location'),
 (@u, 'ADVANTAGE', 15, 'Ningbo''s strong trade and finance sector offers direct internship and employment pathways'),
 (@u, 'ADVANTAGE', 16, 'Private-university admissions flexibility can suit a wider range of academic backgrounds'),
 (@u, 'ADVANTAGE', 17, 'Broad 45-program catalogue allows students to explore adjacent fields such as engineering or the arts'),
 (@u, 'ADVANTAGE', 18, 'Coastal Zhejiang setting with strong transport links to Shanghai and Hangzhou'),
 (@u, 'ADVANTAGE', 19, 'National first-class program recognition in select courses adds credibility to specific majors'),
 (@u, 'ADVANTAGE', 20, 'Two decades of institutional development since its 2001 founding');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'Ruanke Private Finance Colleges', 1, 2024, 'Category-specific ranking (private finance/economics colleges), not the general university ranking -- verify current edition before publishing');
-- ============================================================================
-- 25. North Sichuan Medical College
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('North Sichuan Medical College', '川北医学院',
   'north-sichuan-medical-college', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Nanchong', 'Sichuan',
   1951, 10396, NULL, NULL, 'https://www.nsmc.edu.cn/', 'Provincial medical',
   'North Sichuan Medical College is a public medical university in Nanchong, in the northern Sichuan (Chuanbei) region. It offers undergraduate and postgraduate programs across clinical medicine, basic medicine, public health, nursing, pharmacy and related health sciences, and trains a significant share of the region''s clinical workforce through a network of affiliated teaching hospitals.',
   'The college traces its founding to 1951 as North Sichuan School of Medicine, one of the earliest medical schools established in northern Sichuan after 1949. It was renamed North Sichuan Medical College in 1985 and has since expanded from a medical-specialist school into a multi-disciplinary provincial medical university offering both bachelor''s and master''s degrees.',
   'The college operates across two campuses in Nanchong, in the Shunqing and Gaoping districts, and is affiliated with several teaching hospitals in the city, including its own affiliated hospital, which supports clinical training for medical and nursing students.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Total-student figure (10,396, incl. 7,852 undergraduates) is from an English-language secondary source and should be re-verified against the college''s current official statistics before publishing; faculty count not found and left blank. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'North Sichuan Medical College' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Founded in 1951 as North Sichuan School of Medicine, among the earliest medical schools in northern Sichuan'),
 (@u, 'HIGHLIGHT', 2,  'Renamed North Sichuan Medical College in 1985'),
 (@u, 'HIGHLIGHT', 3,  'Faculties spanning clinical medicine, basic medicine, public health, nursing and pharmacy'),
 (@u, 'HIGHLIGHT', 4,  'Two teaching campuses in Nanchong, in the Shunqing and Gaoping districts'),
 (@u, 'HIGHLIGHT', 5,  'Affiliated with multiple teaching hospitals in Nanchong, including its own affiliated hospital'),
 (@u, 'HIGHLIGHT', 6,  'Offers postgraduate (master''s) programs alongside undergraduate medical degrees'),
 (@u, 'HIGHLIGHT', 7,  'Located in Nanchong, the largest city in the Chuanbei (northern Sichuan) region'),
 (@u, 'HIGHLIGHT', 8,  'Provincial public institution under Sichuan provincial administration'),
 (@u, 'HIGHLIGHT', 9,  'Roughly 10,000 full-time students reported, most in undergraduate clinical programs (approximate -- verify)'),
 (@u, 'HIGHLIGHT', 10, 'Long-standing regional reputation for training clinicians serving northern Sichuan'),
 (@u, 'ADVANTAGE', 11, 'Lower cost of living in Nanchong compared with Chengdu or coastal cities'),
 (@u, 'ADVANTAGE', 12, 'Hands-on clinical exposure through a network of affiliated teaching hospitals'),
 (@u, 'ADVANTAGE', 13, 'Clear pathway into regional healthcare employment across northern Sichuan'),
 (@u, 'ADVANTAGE', 14, 'Smaller, medicine-focused campus environment rather than a large comprehensive university'),
 (@u, 'ADVANTAGE', 15, 'Direct highway and rail links to Chengdu for regional travel'),
 (@u, 'ADVANTAGE', 16, 'Over seven decades of institutional history supporting established clinical partnerships'),
 (@u, 'ADVANTAGE', 17, 'Program range covers clinical medicine, nursing, public health and pharmacy under one roof'),
 (@u, 'ADVANTAGE', 18, 'Option to continue directly into master''s-level study at the same institution'),
 (@u, 'ADVANTAGE', 19, 'Provincial government backing as a stable, long-running public institution'),
 (@u, 'ADVANTAGE', 20, 'Simplified admissions relative to more selective national medical universities');
-- No ranking rows: not found in the general CUAA / Shanghai Ranking national tables surveyed for this
-- fragment. Add a medical-college-specific ranking in the dashboard if one is later confirmed.

-- ============================================================================
-- 26. Shandong Agriculture and Engineering University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Shandong Agriculture and Engineering University', '山东农业工程学院',
   'shandong-agriculture-and-engineering-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Jinan', 'Shandong',
   1953, NULL, NULL, NULL, 'https://www.sdaeu.edu.cn/', 'Provincial undergrad',
   'Shandong Agriculture and Engineering University is a provincial public university in Jinan built around the combination of agricultural and engineering disciplines, offering applied undergraduate programs designed around Shandong''s agricultural-technology and rural-development needs.',
   'The university traces its origins to 1953, when it was established as Shandong Agricultural and Forestry Cadre School under the Shandong provincial government. It underwent several reorganizations over subsequent decades before being consolidated under its current name and undergraduate status; the exact date of that final reorganization is approximate and should be verified before publishing.',
   'The main campus is located at Nongganyuan Road in Licheng District, Jinan. The university has been recognized provincially as a "Green University" and as an advanced institution for agricultural science and technology extension in Shandong.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Total-student and faculty-count figures were not found in the sources checked and are left blank -- verify before publishing. The 1953 founding date and the exact year the institution took its current name/undergraduate status should also be reconfirmed against the university''s official history page. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Shandong Agriculture and Engineering University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Traces its origins to 1953 as Shandong Agricultural and Forestry Cadre School'),
 (@u, 'HIGHLIGHT', 2,  'Distinctive combined "agriculture plus engineering" program model'),
 (@u, 'HIGHLIGHT', 3,  'Main campus in Licheng District, Jinan, the Shandong provincial capital'),
 (@u, 'HIGHLIGHT', 4,  'Recognized provincially as a Green University in Shandong'),
 (@u, 'HIGHLIGHT', 5,  'Approved in 2021 as a support unit for Shandong''s application-oriented undergraduate university program'),
 (@u, 'HIGHLIGHT', 6,  'Ranked 455th nationally in the 2026 CUAA (Alumni Association) China University Rankings'),
 (@u, 'HIGHLIGHT', 7,  'Focus on applied programs tied to agricultural technology and rural development'),
 (@u, 'HIGHLIGHT', 8,  'Provincial public institution directly serving Shandong''s agricultural sector'),
 (@u, 'HIGHLIGHT', 9,  'Located in Jinan, giving access to a major provincial capital''s transport and industry links'),
 (@u, 'HIGHLIGHT', 10, 'Decades-long institutional history under several predecessor names'),
 (@u, 'ADVANTAGE', 11, 'Lower tuition and living costs typical of a provincial (non-Ministry-of-Education) university'),
 (@u, 'ADVANTAGE', 12, 'Location in Jinan gives access to a major transport hub and provincial job market'),
 (@u, 'ADVANTAGE', 13, 'Applied, industry-oriented curriculum suited to practical agricultural and engineering careers'),
 (@u, 'ADVANTAGE', 14, 'Smaller, focused student body relative to Shandong''s largest comprehensive universities'),
 (@u, 'ADVANTAGE', 15, 'Provincial government backing supports continued investment in facilities'),
 (@u, 'ADVANTAGE', 16, 'Combined agriculture and engineering faculties offer interdisciplinary study options'),
 (@u, 'ADVANTAGE', 17, 'Established provincial recognition for science-and-technology extension work'),
 (@u, 'ADVANTAGE', 18, 'Straightforward admissions relative to Shandong''s more selective national universities'),
 (@u, 'ADVANTAGE', 19, 'Regional relevance for students interested in agri-tech, food systems or rural engineering'),
 (@u, 'ADVANTAGE', 20, 'Proximity to Jinan''s broader higher-education and internship ecosystem');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 455, 2026, 'Verify against the current edition before publishing');

-- ============================================================================
-- 27. Shandong First Medical University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Shandong First Medical University', '山东第一医科大学',
   'shandong-first-medical-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Jinan', 'Shandong',
   2019, NULL, NULL, NULL, 'https://www.sdfmu.edu.cn/', 'Provincial medical (new)',
   'Shandong First Medical University (also operating under the name Shandong Academy of Medical Sciences) is a public medical university headquartered in Jinan with a second campus in Tai''an. It is a relatively new institution: it was formally established in February 2019 through the merger of several existing medical institutions, and combines undergraduate and postgraduate medical education with a large affiliated research and clinical base.',
   'The university''s lineage traces back to 1915, when Shandong province established its first government-run medical higher-education institution. That institution passed through several names over the 20th century, becoming Taishan Medical College in 1981. In February 2019, Taishan Medical College merged with the Shandong Academy of Medical Sciences, Shandong Provincial Hospital and Shandong Qianfoshan Hospital to form the current Shandong First Medical University -- so while parts of its lineage are over a century old, the institution under its present name and structure is only a few years old, and this should be made clear to anyone evaluating it as a partner.',
   'The university maintains campuses in both Jinan, where the main administrative campus sits within the Jinan International Medical Center area, and Tai''an, home to the former Taishan Medical College site. Its affiliated hospitals, including Shandong Provincial Hospital, provide a large clinical training base.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Newly merged institution (2019) -- do not present as a centuries-old medical school; pre-2019 history belongs to predecessor institutions. Total-student and faculty-count figures not confidently sourced, left blank -- verify (one secondary source''''s "15,000 faculty" likely conflates university staff with the whole affiliated-hospital system; do not use as-is). Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Shandong First Medical University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Formally established in February 2019 through the merger of Taishan Medical College, the Shandong Academy of Medical Sciences, Shandong Provincial Hospital and Shandong Qianfoshan Hospital'),
 (@u, 'HIGHLIGHT', 2,  'Lineage traces back to 1915, Shandong''s first government-run medical higher-education institution'),
 (@u, 'HIGHLIGHT', 3,  'Predecessor institution renamed Taishan Medical College in 1981'),
 (@u, 'HIGHLIGHT', 4,  'Dual-campus structure: main campus in Jinan, second campus in Tai''an'),
 (@u, 'HIGHLIGHT', 5,  'Main Jinan campus sits within the Jinan International Medical Center area'),
 (@u, 'HIGHLIGHT', 6,  'Also operates under the name Shandong Academy of Medical Sciences'),
 (@u, 'HIGHLIGHT', 7,  'Directly affiliated with Shandong Provincial Hospital, a major clinical training base'),
 (@u, 'HIGHLIGHT', 8,  'Named a Shandong provincial "high-level university" development project in 2020'),
 (@u, 'HIGHLIGHT', 9,  'One of the largest medical research and clinical institutions in Shandong province'),
 (@u, 'HIGHLIGHT', 10, 'Offers programs from undergraduate through postgraduate and postdoctoral research'),
 (@u, 'ADVANTAGE', 11, 'Direct clinical training access through a large network of affiliated hospitals'),
 (@u, 'ADVANTAGE', 12, 'Jinan location offers access to Shandong''s provincial capital and its healthcare infrastructure'),
 (@u, 'ADVANTAGE', 13, 'Combines teaching, provincial hospital care and medical research under one institution'),
 (@u, 'ADVANTAGE', 14, 'Recent, well-resourced merger backed by significant provincial investment'),
 (@u, 'ADVANTAGE', 15, 'Two-campus structure gives students exposure to both Jinan and Tai''an clinical settings'),
 (@u, 'ADVANTAGE', 16, 'Postgraduate and postdoctoral research pathways available without changing institutions'),
 (@u, 'ADVANTAGE', 17, 'Provincial "high-level university" status signals ongoing government investment'),
 (@u, 'ADVANTAGE', 18, 'Scale of the merged institution supports a broad range of medical specialties'),
 (@u, 'ADVANTAGE', 19, 'Jinan''s transport links ease access to the rest of Shandong and to Beijing'),
 (@u, 'ADVANTAGE', 20, 'Century-old predecessor lineage lends academic depth despite the young corporate structure');
-- No ranking rows: the merged institution (est. 2019) is too new to appear consistently in the
-- general CUAA / Shanghai Ranking tables surveyed for this fragment. Check a current
-- medical-university-specific ranking before publishing and add rows via the dashboard.

-- ============================================================================
-- 28. Shanghai University of Political Science and Law
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Shanghai University of Political Science and Law', '上海政法学院',
   'shanghai-university-of-political-science-and-law', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Shanghai', 'Shanghai',
   1984, NULL, NULL, NULL, 'https://www.shupl.edu.cn/', 'Municipal law-focused',
   'Shanghai University of Political Science and Law is a municipal public university in Shanghai specializing in law, public security, political science and related social-science disciplines. It trains a significant share of Shanghai''s legal, judicial and public-administration workforce alongside more general undergraduate programs.',
   'The university was formed in 1984 from the merger of three predecessor institutions: the Shanghai Administrative Institute of Politics and Law, the Shanghai Judicial College, and the law school of Shanghai University. It began offering undergraduate programs in 1993 and master''s programs in 1998, and in September 2004 the Shanghai Municipal Government approved its establishment as an independent university under its current English name.',
   'The university operates as a municipal (rather than national Ministry-of-Education) public institution in Shanghai, with programs concentrated in law, criminal justice, political science and public administration.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Total-student and faculty-count figures were not confidently sourced within this research pass and are left blank -- verify before publishing. Note the institution''s English name uses "University" but its formal PRC classification status should be double-checked before it is described that way in any staff-facing material. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Shanghai University of Political Science and Law' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Formed in 1984 from three predecessor institutions in law and public administration'),
 (@u, 'HIGHLIGHT', 2,  'Began undergraduate education in 1993, master''s programs in 1998'),
 (@u, 'HIGHLIGHT', 3,  'Established as an independent municipal university in September 2004'),
 (@u, 'HIGHLIGHT', 4,  'Located in Shanghai, one of China''s largest legal and financial services markets'),
 (@u, 'HIGHLIGHT', 5,  'Specializes in law, criminal justice, political science and public administration'),
 (@u, 'HIGHLIGHT', 6,  'Municipal public institution under the Shanghai Municipal Government'),
 (@u, 'HIGHLIGHT', 7,  'Trains a notable share of Shanghai''s judicial and public-security personnel'),
 (@u, 'HIGHLIGHT', 8,  'Offers programs across bachelor''s and master''s levels in legal and social-science fields'),
 (@u, 'HIGHLIGHT', 9,  'Recognized for international legal-education partnerships and training programs'),
 (@u, 'HIGHLIGHT', 10, 'Over four decades of institutional history in Shanghai legal education'),
 (@u, 'ADVANTAGE', 11, 'Shanghai location offers direct access to major law firms, courts and multinational employers'),
 (@u, 'ADVANTAGE', 12, 'Specialized law and public-administration focus rather than a broad general curriculum'),
 (@u, 'ADVANTAGE', 13, 'Strong ties into Shanghai''s judicial and public-security career pathways'),
 (@u, 'ADVANTAGE', 14, 'Municipal capital-city location without the cost premium of a top-tier national university'),
 (@u, 'ADVANTAGE', 15, 'Established international legal-education exchange programs'),
 (@u, 'ADVANTAGE', 16, 'Master''s-level study available on campus for continued legal specialization'),
 (@u, 'ADVANTAGE', 17, 'Shanghai''s transport and internship ecosystem supports part-time and post-study work'),
 (@u, 'ADVANTAGE', 18, 'Focused faculty base in law-adjacent disciplines rather than a sprawling multi-subject campus'),
 (@u, 'ADVANTAGE', 19, 'Municipal-government backing supports continued facility investment'),
 (@u, 'ADVANTAGE', 20, 'Four decades of accumulated relationships with Shanghai''s legal and public sector');
-- No ranking rows: not consistently found in the general CUAA / Shanghai Ranking national tables
-- surveyed for this fragment. If a law-specialty ranking is later confirmed, add it via the dashboard.

-- ============================================================================
-- 29. Shenyang City College
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Shenyang City College', '沈阳城市学院',
   'shenyang-city-college', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PRIVATE', 'Shenyang', 'Liaoning',
   2001, 11000, NULL, NULL, 'https://www.shenyangcu.edu.cn/', 'Private undergrad',
   'Shenyang City College is a private undergraduate university in Shenyang, Liaoning. It began as a dependent "independent college" (a private institution operated in partnership with a public university) and has since completed the national conversion process to become a fully independent, privately registered undergraduate university in its own right.',
   'The college was established in 2001 as the School of Science and Engineering, Shenyang University, and in 2004 was formally confirmed as one of China''s first officially recognized independent colleges -- a category of private institution co-branded with a public "parent" university. In April 2013, following national policy requiring these independent colleges to either fully separate or be dissolved, the Ministry of Education approved its conversion into a wholly independent, privately registered ordinary undergraduate institution, renamed Shenyang City College. It no longer operates as a dependent unit of Shenyang University.',
   'The college operates as a standalone private campus in Shenyang, Liaoning, with roughly 11,000 full-time undergraduate students reported.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Verified the independent-college-to-fully-independent-private-university conversion is complete (MOE approval, April 2013) -- describe as a standalone private university, not a Shenyang University-affiliated branch. Faculty and international-student figures not found -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Shenyang City College' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Established in 2001 as the School of Science and Engineering, Shenyang University'),
 (@u, 'HIGHLIGHT', 2,  'Confirmed in 2004 as one of China''s first officially recognized independent colleges'),
 (@u, 'HIGHLIGHT', 3,  'Converted in April 2013 into a fully independent, privately registered undergraduate university'),
 (@u, 'HIGHLIGHT', 4,  'No longer operates as a Shenyang University-affiliated branch'),
 (@u, 'HIGHLIGHT', 5,  'Approximately 11,000 full-time undergraduate students'),
 (@u, 'HIGHLIGHT', 6,  'Located in Shenyang, the capital of Liaoning province'),
 (@u, 'HIGHLIGHT', 7,  'Selected as a national "1+X Certificate" pilot institution in 2020'),
 (@u, 'HIGHLIGHT', 8,  'Offers a broad range of applied undergraduate programs'),
 (@u, 'HIGHLIGHT', 9,  'Recognized regionally among Liaoning''s private undergraduate institutions'),
 (@u, 'HIGHLIGHT', 10, 'Privately owned and operated ordinary undergraduate institution'),
 (@u, 'ADVANTAGE', 11, 'Shenyang location gives access to a major northeastern Chinese industrial and job market'),
 (@u, 'ADVANTAGE', 12, 'Private-university admissions flexibility relative to public national universities'),
 (@u, 'ADVANTAGE', 13, 'Applied program mix aimed at direct employment outcomes'),
 (@u, 'ADVANTAGE', 14, 'Participation in national vocational-certificate pilot programs (1+X)'),
 (@u, 'ADVANTAGE', 15, 'Fully independent institutional status removes ambiguity about degree issuance'),
 (@u, 'ADVANTAGE', 16, 'Moderate cost of living in Shenyang compared with China''s coastal megacities'),
 (@u, 'ADVANTAGE', 17, 'Established campus infrastructure built up over two decades'),
 (@u, 'ADVANTAGE', 18, 'Simplified private-institution admissions and enrollment process'),
 (@u, 'ADVANTAGE', 19, 'Liaoning''s manufacturing and logistics base offers internship opportunities'),
 (@u, 'ADVANTAGE', 20, 'Smaller institutional scale can mean more accessible student support services');
-- No ranking rows: not found in the general CUAA / Shanghai Ranking national tables surveyed for
-- this fragment. Add a private-university-specific ranking via the dashboard if one is confirmed.

-- ============================================================================
-- 30. Sias University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Sias University', '西亚斯学院',
   'sias-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PRIVATE', 'Xinzheng', 'Henan',
   1998, NULL, NULL, NULL, 'https://www.sias.edu.cn/', 'Private undergrad',
   'Sias University is a private undergraduate university in Xinzheng, part of the greater Zhengzhou metropolitan area in Henan province. It has a long history of Sino-American cooperative education and has changed its formal registration status and name more than once as Chinese policy on this category of institution evolved.',
   'The institution began in 1998 as a Sino-foreign cooperative program, established with Zhengzhou University of Technology (later Zhengzhou University) and Fort Hays State University (Kansas, USA), initially operating as Zhengzhou University Sias International College. Over the following two decades it was known at various points as Sias International University and as Zhengzhou University Sias International College. In December 2018, the Ministry of Education approved its separation from Zhengzhou University into a standalone private non-profit institution under the name Sias University, its current official name. Any older references to "Sias International University" or "Zhengzhou Shengda University of Economics, Business and Management" describe earlier stages of this same institution and should be treated as historical, not current.',
   'The main campus is located in Xinzheng, Henan, within commuting distance of central Zhengzhou, and retains strong ties to its Sino-American cooperative-education origins.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Confirmed current official name is "Sias University" (separated from Zhengzhou University, re-registered as standalone private institution, MOE approval December 2018) -- do not use "Sias International University" or the Zhengzhou-affiliated name without noting it is historical. Total-student and faculty-count figures not confidently sourced -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Sias University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Founded in 1998 as a Sino-American cooperative institution with Fort Hays State University'),
 (@u, 'HIGHLIGHT', 2,  'Originally established jointly with Zhengzhou University of Technology'),
 (@u, 'HIGHLIGHT', 3,  'Separated into a standalone private university, renamed Sias University, in December 2018'),
 (@u, 'HIGHLIGHT', 4,  'Located in Xinzheng, within the greater Zhengzhou metropolitan area'),
 (@u, 'HIGHLIGHT', 5,  'Ranked 71st nationally in the 2026 Shanghai Ranking (Soft Science) China private-university table'),
 (@u, 'HIGHLIGHT', 6,  'History of Sino-foreign cooperative curriculum design dating back over two decades'),
 (@u, 'HIGHLIGHT', 7,  'Operates as a private non-profit institution under current Ministry of Education registration'),
 (@u, 'HIGHLIGHT', 8,  'Broad undergraduate program offering across business, humanities and applied sciences'),
 (@u, 'HIGHLIGHT', 9,  'One of the more internationally connected private universities in Henan province'),
 (@u, 'HIGHLIGHT', 10, 'Campus located close to Zhengzhou Xinzheng International Airport'),
 (@u, 'ADVANTAGE', 11, 'Long-standing American-style curriculum influence from its Fort Hays State University origins'),
 (@u, 'ADVANTAGE', 12, 'Proximity to Zhengzhou, a major Central China transport and logistics hub'),
 (@u, 'ADVANTAGE', 13, 'Close to Zhengzhou Xinzheng International Airport for international students'),
 (@u, 'ADVANTAGE', 14, 'Established international exchange and cooperative-education relationships'),
 (@u, 'ADVANTAGE', 15, 'Private-university admissions flexibility relative to public national universities'),
 (@u, 'ADVANTAGE', 16, 'Lower cost of living in Henan relative to China''s coastal provinces'),
 (@u, 'ADVANTAGE', 17, 'Two decades of experience hosting foreign faculty and cooperative programs'),
 (@u, 'ADVANTAGE', 18, 'Broad program mix supports flexible major selection'),
 (@u, 'ADVANTAGE', 19, 'Growing recognition in national private-university rankings'),
 (@u, 'ADVANTAGE', 20, 'Independent, fully Chinese-registered status simplifies degree recognition');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'Soft Science China Private Univ', 71, 2026, 'Fluctuates year to year (52nd in 2023, 83rd in 2024, 60th in 2025) -- verify against the current edition before publishing');

-- ============================================================================
-- 31. Sichuan College of Architectural Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Sichuan College of Architectural Technology', '四川建筑职业技术学院',
   'sichuan-college-of-architectural-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Deyang', 'Sichuan',
   1956, 16544, NULL, 1015, 'https://www.scac.edu.cn/', 'Vocational college',
   'Sichuan College of Architectural Technology is a public vocational and technical college in Deyang, Sichuan, affiliated with the Sichuan Provincial Department of Housing and Urban-Rural Development. It is important to be explicit that this is a vocational, associate-degree-granting institution (gaozhi gaozhuan) -- it does not award bachelor''s degrees, and should never be catalogued or marketed to applicants as a bachelor''s-degree university.',
   'The college''s roots trace to the Chengdu Urban Construction Engineering School, founded in 1956, which was renamed Chengdu Construction Engineering School in 1958 and relocated to Deyang in 1963. In April 2001, the Sichuan provincial government approved the merger of the former Sichuan Provincial Construction Engineering School, Sichuan Provincial Urban Construction School and Sichuan Provincial Construction Workers University to form the present-day Sichuan College of Architectural Technology, focused on construction and built-environment vocational training.',
   'The college operates two campuses: the main Deyang campus and a Chengdu campus. As of late 2024 it reported roughly 16,500 full-time students and just over 1,000 full-time teaching staff.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. IMPORTANT: this is a vocational/associate-degree institution (高职高专), not a bachelor''s-degree university -- do not apply a bachelor-degree ranking_tier or present it as equivalent to a four-year university in any student-facing material. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Sichuan College of Architectural Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Vocational, associate-degree college (gaozhi gaozhuan) -- does not award bachelor''s degrees'),
 (@u, 'HIGHLIGHT', 2,  'Roots trace to Chengdu Urban Construction Engineering School, founded in 1956'),
 (@u, 'HIGHLIGHT', 3,  'Relocated to Deyang in 1963; present institution formed by a 2001 provincial merger'),
 (@u, 'HIGHLIGHT', 4,  'Affiliated with the Sichuan Provincial Department of Housing and Urban-Rural Development'),
 (@u, 'HIGHLIGHT', 5,  'Focused specifically on construction, architecture and built-environment vocational training'),
 (@u, 'HIGHLIGHT', 6,  'Operates two campuses, in Deyang and Chengdu'),
 (@u, 'HIGHLIGHT', 7,  'Roughly 16,500 full-time students as of late 2024'),
 (@u, 'HIGHLIGHT', 8,  'Just over 1,000 full-time teaching staff as of late 2024'),
 (@u, 'HIGHLIGHT', 9,  'Long-standing provincial reputation in construction-trades vocational education'),
 (@u, 'HIGHLIGHT', 10, 'Public institution under direct Sichuan provincial administration'),
 (@u, 'ADVANTAGE', 11, 'Direct, industry-aligned pathway into construction and built-environment careers'),
 (@u, 'ADVANTAGE', 12, 'Shorter, lower-cost associate-degree programs relative to a four-year bachelor''s degree'),
 (@u, 'ADVANTAGE', 13, 'Chengdu campus access alongside the main Deyang campus'),
 (@u, 'ADVANTAGE', 14, 'Practical, hands-on technical training suited to applied construction trades'),
 (@u, 'ADVANTAGE', 15, 'Strong ties to Sichuan''s provincial construction and housing sector'),
 (@u, 'ADVANTAGE', 16, 'Lower cost of living in Deyang relative to Chengdu'),
 (@u, 'ADVANTAGE', 17, 'Established employer relationships through decades of vocational-training experience'),
 (@u, 'ADVANTAGE', 18, 'Clear, time-efficient route for applicants seeking a technical/associate credential rather than a bachelor''s degree'),
 (@u, 'ADVANTAGE', 19, 'Straightforward admissions typical of vocational colleges'),
 (@u, 'ADVANTAGE', 20, 'Deyang''s proximity to Chengdu keeps students within reach of a major metropolitan job market');
-- No ranking rows: this is a vocational/associate-degree institution (高职高专) and is not evaluated
-- in bachelor-degree ranking tables such as CUAA or Shanghai Ranking. Add a vocational-college-specific
-- ranking via the dashboard if one is confirmed.

-- ============================================================================
-- 32. Southwestern University of Finance and Economics, Tianfu College
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Southwestern University of Finance and Economics, Tianfu College', '西南财经大学天府学院',
   'southwestern-university-of-finance-and-economics-tianfu-college', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PRIVATE', 'Mianyang', 'Sichuan',
   2006, NULL, NULL, 1102, 'https://tfswufe.edu.cn/', 'Private indep. college',
   'Tianfu College is a private "independent college" (duli xueyuan) affiliated with Southwestern University of Finance and Economics (SWUFE), one of China''s leading national finance and economics universities. It is legally and financially a separate, privately funded institution from SWUFE, though it was originally co-founded with SWUFE and continues to use the SWUFE name and academic model under license -- this affiliation and its independent-college status should always be stated clearly, not implied as full equivalence with SWUFE itself.',
   'Tianfu College was established in 2006 with Ministry of Education approval as a private, full-time regular-undergraduate independent college co-founded by Southwestern University of Finance and Economics together with private investment partners. The identity of the private investment partner listed as co-founder changed in April 2022, per a Ministry of Education notice, from one Sichuan-based education investment company to another; SWUFE''s role as academic co-founder was not affected by that change.',
   'The college operates across three campuses in Mianyang, Chengdu and Deyang, organized into thirteen secondary colleges offering both undergraduate and associate-degree programs, with a combined total site area of over 1,900 acres.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Must always be described as a private independent college affiliated with SWUFE, not as SWUFE itself -- a distinct legal entity. Total-student figure not found -- verify; faculty count (1,102 full-time, 177 part-time) is as of November 2024 and should be reconfirmed. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Southwestern University of Finance and Economics, Tianfu College' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Private independent college affiliated with Southwestern University of Finance and Economics (SWUFE)'),
 (@u, 'HIGHLIGHT', 2,  'Established in 2006 with Ministry of Education approval'),
 (@u, 'HIGHLIGHT', 3,  'Ranked 6th nationally in the 2025 CUAA China Independent Colleges ranking'),
 (@u, 'HIGHLIGHT', 4,  'Ranked 1st nationally among finance-category independent colleges in the 2025 CUAA ranking'),
 (@u, 'HIGHLIGHT', 5,  'Three campuses in Mianyang, Chengdu and Deyang, over 1,900 acres combined'),
 (@u, 'HIGHLIGHT', 6,  'Thirteen secondary colleges offering 44 undergraduate and 32 associate-degree majors'),
 (@u, 'HIGHLIGHT', 7,  'Over 1,100 full-time faculty as of November 2024'),
 (@u, 'HIGHLIGHT', 8,  'Curriculum modeled on SWUFE''s finance and economics academic approach'),
 (@u, 'HIGHLIGHT', 9,  'Legally and financially distinct private entity from SWUFE itself'),
 (@u, 'HIGHLIGHT', 10, 'Private co-founding investment entity changed in 2022 per Ministry of Education notice'),
 (@u, 'ADVANTAGE', 11, 'Access to a SWUFE-modeled finance and economics curriculum at private-college tuition levels'),
 (@u, 'ADVANTAGE', 12, 'Top-ranked among China''s finance-focused independent colleges'),
 (@u, 'ADVANTAGE', 13, 'Multi-campus presence across Mianyang, Chengdu and Deyang for regional flexibility'),
 (@u, 'ADVANTAGE', 14, 'Broad program range spanning undergraduate and associate-degree tracks'),
 (@u, 'ADVANTAGE', 15, 'Sichuan location offers proximity to Chengdu''s finance and business sector'),
 (@u, 'ADVANTAGE', 16, 'Private-college admissions flexibility relative to SWUFE''s main campus'),
 (@u, 'ADVANTAGE', 17, 'Large, well-resourced faculty base for a private independent college'),
 (@u, 'ADVANTAGE', 18, 'Established brand recognition through the SWUFE academic affiliation'),
 (@u, 'ADVANTAGE', 19, 'Lower cost of living in Mianyang relative to Chengdu'),
 (@u, 'ADVANTAGE', 20, 'Consistent top-tier standing among China''s independent colleges over multiple ranking years');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China Independent Colleges Ranking', 6, 2025, 'Overall national rank among independent colleges -- verify against the current edition before publishing'),
 (@u, 'CUAA Indep. Colleges (Finance)', 1, 2025, 'Rank within finance-focused independent colleges specifically -- verify against the current edition before publishing');

-- ============================================================================
-- 33. Taiyuan Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Taiyuan Normal University', '太原师范学院',
   'taiyuan-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Jinzhong', 'Shanxi',
   1999, 25600, NULL, NULL, 'https://www.tynu.edu.cn/', 'Provincial normal univ.',
   'Taiyuan Normal University is a public teacher-education-focused university. Despite the name "Taiyuan," its main campus is not located in Taiyuan city -- the university''s new/main campus sits in the university town in Jinzhong, a separate prefecture-level city adjacent to Taiyuan. This is a genuine, well-documented feature of the institution and not a data error: the name reflects the university''s historical origin in Taiyuan, while its present-day main campus operations are centered in Jinzhong.',
   'The university was established in March 1999 through the merger of Shanxi University Normal College, Taiyuan Teachers College and the Shanxi Provincial Institute of Education, all approved by the national Ministry of Education. It retains three older campuses (north, middle and south) plus a larger new campus built more recently in the Jinzhong university town area, which is now the university''s primary campus.',
   'The university operates across four campuses: three older campuses totaling roughly 361,000 square meters, and a newer, larger campus of roughly 872,000 square meters in the university town in the Yuci district of Jinzhong -- which is why the catalog records its city as Jinzhong rather than Taiyuan despite the institution''s name.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. City deliberately set to Jinzhong, not Taiyuan -- the university''''s main/new campus is physically in the Jinzhong university town despite the institution''''s name. Do not "correct" this without re-checking current campus locations. Total-students (~25,600) should be reconfirmed; faculty count not found. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Taiyuan Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Established in March 1999 through the merger of three Shanxi teacher-education institutions'),
 (@u, 'HIGHLIGHT', 2,  'Main/new campus is located in the university town in Jinzhong, not in Taiyuan city'),
 (@u, 'HIGHLIGHT', 3,  'Four campuses in total: three older sites plus a larger newer campus'),
 (@u, 'HIGHLIGHT', 4,  'Roughly 24,000 full-time undergraduates and 1,559 postgraduates reported by the university'),
 (@u, 'HIGHLIGHT', 5,  'Formed from Shanxi University Normal College, Taiyuan Teachers College and Shanxi Provincial Institute of Education'),
 (@u, 'HIGHLIGHT', 6,  'Students drawn from 28 provinces, municipalities and autonomous regions'),
 (@u, 'HIGHLIGHT', 7,  'Provincial public institution under Shanxi provincial administration'),
 (@u, 'HIGHLIGHT', 8,  'Core focus on teacher education alongside a broader arts-and-sciences curriculum'),
 (@u, 'HIGHLIGHT', 9,  'New campus spans roughly 872,000 square meters in the Yuci district of Jinzhong'),
 (@u, 'HIGHLIGHT', 10, 'Over 25 years as a merged, unified institution'),
 (@u, 'ADVANTAGE', 11, 'Lower cost of living in Jinzhong relative to Taiyuan or China''s coastal cities'),
 (@u, 'ADVANTAGE', 12, 'Location in a dedicated university town with modern, purpose-built campus facilities'),
 (@u, 'ADVANTAGE', 13, 'Close proximity to Taiyuan, Shanxi''s provincial capital, for internships and transport links'),
 (@u, 'ADVANTAGE', 14, 'Established teacher-education pathway into Shanxi''s education sector'),
 (@u, 'ADVANTAGE', 15, 'Broad undergraduate program mix beyond teacher education alone'),
 (@u, 'ADVANTAGE', 16, 'Sizable student body offers a wide range of student clubs and campus life'),
 (@u, 'ADVANTAGE', 17, 'Provincial government backing supports continued campus investment'),
 (@u, 'ADVANTAGE', 18, 'Modern new-campus infrastructure relative to many older provincial universities'),
 (@u, 'ADVANTAGE', 19, 'National student intake from 28 provinces broadens the campus community'),
 (@u, 'ADVANTAGE', 20, 'Straightforward admissions typical of a provincial (non-national-tier) university');
-- No ranking rows: not found in the general CUAA / Shanghai Ranking national tables surveyed for
-- this fragment. Add a ranking via the dashboard if one is later confirmed.

-- ============================================================================
-- 34. Taizhou University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Taizhou University', '台州学院',
   'taizhou-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Taizhou', 'Zhejiang',
   1907, NULL, NULL, NULL, 'https://www.tzc.edu.cn/', 'Provincial undergrad',
   'Taizhou University is a public comprehensive university serving Taizhou, a prefecture-level city on the coast of Zhejiang province. Its main campus is physically located in Linhai, a county-level city administered under Taizhou, which is standard for how Zhejiang''s prefecture-level institutions are often sited and should not be read as an inconsistency.',
   'The university''s lineage traces back to 1907, to early normal-school predecessors in the Taizhou area. The present-day Taizhou University was formed through a later merger of several local higher-education institutions -- including teacher-education and vocational colleges serving the Taizhou region -- and adopted its current name and comprehensive-university structure; the exact merger year should be reconfirmed against the university''s official history before publishing.',
   'The main campus is located in Linhai, within Taizhou prefecture-level city, Zhejiang province.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Total-student and faculty-count figures were inconsistent across sources (estimates from ~7,000 undergrads to 10,000-15,000 total) and left blank pending direct verification. The exact year the present-day university formed via merger (distinct from its 1907 lineage date) should also be confirmed. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Taizhou University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Institutional lineage traces back to 1907 in the Taizhou region'),
 (@u, 'HIGHLIGHT', 2,  'Main campus located in Linhai, within Taizhou prefecture-level city, Zhejiang'),
 (@u, 'HIGHLIGHT', 3,  'Formed as a comprehensive public university through the merger of local Taizhou-area colleges'),
 (@u, 'HIGHLIGHT', 4,  'Public institution under Zhejiang provincial and Taizhou municipal administration'),
 (@u, 'HIGHLIGHT', 5,  'Serves Taizhou, a significant coastal manufacturing and trade hub in Zhejiang'),
 (@u, 'HIGHLIGHT', 6,  'Comprehensive undergraduate program offering across arts, sciences and applied fields'),
 (@u, 'HIGHLIGHT', 7,  'Recognized in Zhejiang provincial university rankings'),
 (@u, 'HIGHLIGHT', 8,  'Over a century of educational lineage in the Taizhou region'),
 (@u, 'HIGHLIGHT', 9,  'Mid-sized institution with a full range of undergraduate faculties'),
 (@u, 'HIGHLIGHT', 10, 'Located in one of Zhejiang''s major coastal prefecture-level cities'),
 (@u, 'ADVANTAGE', 11, 'Coastal Zhejiang location with strong regional manufacturing and trade employers'),
 (@u, 'ADVANTAGE', 12, 'Lower cost of living relative to Hangzhou or Shanghai'),
 (@u, 'ADVANTAGE', 13, 'Comprehensive program mix suited to a range of applicant interests'),
 (@u, 'ADVANTAGE', 14, 'Long institutional lineage supporting established local employer relationships'),
 (@u, 'ADVANTAGE', 15, 'Direct access to Taizhou''s private-sector manufacturing and trading economy'),
 (@u, 'ADVANTAGE', 16, 'Provincial government backing supports continued campus investment'),
 (@u, 'ADVANTAGE', 17, 'Zhejiang''s strong transport network eases travel to Hangzhou, Ningbo and Shanghai'),
 (@u, 'ADVANTAGE', 18, 'Public-university tuition levels relative to Zhejiang''s private institutions'),
 (@u, 'ADVANTAGE', 19, 'Regional reputation as Taizhou''s primary comprehensive university'),
 (@u, 'ADVANTAGE', 20, 'Established local alumni network across Taizhou''s business community');
-- No ranking rows: not found in the general CUAA / Shanghai Ranking national tables surveyed for
-- this fragment. Add a ranking via the dashboard if one is later confirmed.

-- ============================================================================
-- 35. Tianjin University of Finance and Economics
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Tianjin University of Finance and Economics', '天津财经大学',
   'tianjin-university-of-finance-and-economics', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Tianjin', 'Tianjin',
   1958, 12000, NULL, NULL, 'https://www.tjufe.edu.cn/', 'Provincial key finance',
   'Tianjin University of Finance and Economics is a public university in the Hexi district of Tianjin, among the earlier Chinese institutions to offer applied economics and business administration programs. It offers a full range of degrees from bachelor''s through doctoral level, concentrated in economics, finance, business, management and law.',
   'The university was founded in 1958 and has operated continuously since as a dedicated finance-and-economics institution in Tianjin, one of China''s four direct-administered municipalities. It has expanded over subsequent decades from a specialist finance college into a broader university offering economics, management, law, science and engineering programs.',
   'The main campus is located in the Hexi district of Tianjin, with roughly 12,000 students split between undergraduate and postgraduate programs.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. Student total (~12,000: ~10,000 undergraduate, ~2,000 postgraduate) and both ranking figures should be reconfirmed against the current academic year before publishing; faculty count not found. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Tianjin University of Finance and Economics' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Founded in 1958 as a dedicated finance-and-economics institution'),
 (@u, 'HIGHLIGHT', 2,  'Located in the Hexi district of Tianjin, one of China''s four direct-administered municipalities'),
 (@u, 'HIGHLIGHT', 3,  'Ranked 153rd nationally in the 2025 CUAA China University Rankings'),
 (@u, 'HIGHLIGHT', 4,  'Ranked 16th nationally among finance-category universities in the Shanghai Ranking (Soft Science) table'),
 (@u, 'HIGHLIGHT', 5,  'Offers degrees from bachelor''s through doctoral level'),
 (@u, 'HIGHLIGHT', 6,  'Programs span economics, finance, business, management, law, science and engineering'),
 (@u, 'HIGHLIGHT', 7,  'Roughly 12,000 students, split between undergraduate and postgraduate study'),
 (@u, 'HIGHLIGHT', 8,  'One of China''s earlier institutions to offer applied economics and business administration'),
 (@u, 'HIGHLIGHT', 9,  'Public institution with over six decades of continuous operation'),
 (@u, 'HIGHLIGHT', 10, 'Located in a major northern Chinese port city and financial center'),
 (@u, 'ADVANTAGE', 11, 'Tianjin location offers direct access to a major port city and financial hub'),
 (@u, 'ADVANTAGE', 12, 'Specialized finance-and-economics reputation built over more than six decades'),
 (@u, 'ADVANTAGE', 13, 'Top-20 national standing among finance-category universities'),
 (@u, 'ADVANTAGE', 14, 'Proximity to Beijing via Tianjin''s high-speed rail links'),
 (@u, 'ADVANTAGE', 15, 'Broad postgraduate offering allows direct progression from undergraduate study'),
 (@u, 'ADVANTAGE', 16, 'Direct-administered-municipality status brings stronger public investment than an ordinary provincial city'),
 (@u, 'ADVANTAGE', 17, 'Established recruiting relationships with Tianjin''s financial and logistics employers'),
 (@u, 'ADVANTAGE', 18, 'Moderate cost of living in Tianjin relative to Beijing or Shanghai'),
 (@u, 'ADVANTAGE', 19, 'Comprehensive program range beyond finance alone, including law and engineering'),
 (@u, 'ADVANTAGE', 20, 'Solid mid-tier national ranking with a clear finance specialization for applicants targeting that field');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 153, 2025, 'Verify against the current edition before publishing'),
 (@u, 'Soft Science China Finance Univ', 16, 2025, 'Rank within the finance-university category specifically -- verify against the current edition before publishing');

-- ============================================================================
-- 36. Wenzhou University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Wenzhou University', '温州大学',
   'wenzhou-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Wenzhou', 'Zhejiang',
   1956, 24960, NULL, NULL, 'https://www.wzu.edu.cn/', 'Municipal comprehensive',
   'Wenzhou University is a public comprehensive university owned by the Wenzhou Municipal People''s Government, offering a broad range of undergraduate and postgraduate programs. It is a distinct institution from Wenzhou-Kean University -- the Sino-US cooperative university also located in Wenzhou and jointly operated with Kean University (USA) -- and the two should never be treated as the same catalog entry or confused with one another in student-facing material; Wenzhou-Kean University is handled separately in another catalog batch.',
   'The university was formed through the amalgamation of Wenzhou Normal College, established in 1956, and the former Wenzhou University, established in 1984. The merged institution retained the name Wenzhou University and has since grown into a comprehensive municipal university spanning multiple disciplines.',
   'The university operates as a comprehensive campus in Wenzhou, Zhejiang, with roughly 25,000 students, and is administered directly by the Wenzhou Municipal People''s Government rather than a provincial or national ministry.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22 from public sources for partner catalog build-out. IMPORTANT: distinct institution from Wenzhou-Kean University (wku.edu.cn), a separate Sino-US cooperative university also based in Wenzhou -- do not merge or cross-reference these two catalog entries. Faculty count not found -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Wenzhou University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Formed from the merger of Wenzhou Normal College (1956) and the former Wenzhou University (1984)'),
 (@u, 'HIGHLIGHT', 2,  'Ranked 159th nationally in the 2025 CUAA China University Rankings'),
 (@u, 'HIGHLIGHT', 3,  'Rated a 2025 CUAA three-star (3-star) regionally prominent university'),
 (@u, 'HIGHLIGHT', 4,  'Classified by CUAA as a 2025 China Regional Research University'),
 (@u, 'HIGHLIGHT', 5,  'Roughly 25,000 students across undergraduate and postgraduate programs'),
 (@u, 'HIGHLIGHT', 6,  'Owned and administered directly by the Wenzhou Municipal People''s Government'),
 (@u, 'HIGHLIGHT', 7,  'Distinct institution from Wenzhou-Kean University, the separate Sino-US cooperative university in Wenzhou'),
 (@u, 'HIGHLIGHT', 8,  'Comprehensive program offering spanning sciences, engineering, humanities and business'),
 (@u, 'HIGHLIGHT', 9,  'Industrial Engineering program ranked 3rd nationally per CUAA 2025 subject rankings'),
 (@u, 'HIGHLIGHT', 10, 'Located in Wenzhou, a major private-enterprise and manufacturing hub in Zhejiang'),
 (@u, 'ADVANTAGE', 11, 'Wenzhou''s strong private-enterprise economy offers direct entrepreneurship and business exposure'),
 (@u, 'ADVANTAGE', 12, 'Top-160 national ranking with clear regional strength in Zhejiang'),
 (@u, 'ADVANTAGE', 13, 'Comprehensive program mix suited to a wide range of applicant interests'),
 (@u, 'ADVANTAGE', 14, 'Nationally ranked Industrial Engineering program (3rd nationally per CUAA)'),
 (@u, 'ADVANTAGE', 15, 'Municipal-government backing supports continued campus and program investment'),
 (@u, 'ADVANTAGE', 16, 'Zhejiang''s transport network eases travel to Hangzhou, Ningbo and Shanghai'),
 (@u, 'ADVANTAGE', 17, 'Sizable, well-established student body of roughly 25,000'),
 (@u, 'ADVANTAGE', 18, 'Direct access to Wenzhou''s manufacturing, trade and private-business employers'),
 (@u, 'ADVANTAGE', 19, 'Moderate cost of living relative to Hangzhou or Shanghai'),
 (@u, 'ADVANTAGE', 20, 'Clearly distinct identity from Wenzhou-Kean University avoids applicant confusion between the two Wenzhou institutions');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 159, 2025, 'Verify against the current edition before publishing');
-- ============================================================================
-- 37. Wenzhou-Kean University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Wenzhou-Kean University', '温州肯恩大学',
   'wenzhou-kean-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Wenzhou', 'Zhejiang',
   2011, 8500, NULL, NULL, 'https://www.wku.edu.cn/en', 'Sino-foreign cooperative',
   'Wenzhou-Kean University (WKU) is a Sino-US cooperative university jointly established by Wenzhou University and Kean University (New Jersey, USA). It is a legally independent, non-profit Chinese-foreign cooperatively-run institution, teaching an American-style liberal arts curriculum in English on a purpose-built campus in Wenzhou.',
   'The China Ministry of Education approved preparation for the university in November 2011; the campus opened and enrolled its first preparatory class in 2012, and WKU was formally established as an independent institution in March 2014. It operates under a joint governance structure with both founding universities.',
   'The campus sits in the Zhuang Yuan Fang area of Wenzhou, built specifically for the joint venture with US-style classroom and residential-college facilities. Instruction follows a bilingual, American-curriculum model with courses taught in English by a mixed Chinese and international faculty.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Sino-US cooperative university -- distinct legal entity from Wenzhou University (a separate institution, handled in another batch); do not merge records. Enrollment figure is an approximate target from public sources -- verify current headcount, faculty count and international-student count before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Wenzhou-Kean University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Sino-US cooperative university jointly run by Wenzhou University and Kean University (USA)'),
 (@u, 'HIGHLIGHT', 2,  'Independent, non-profit Chinese-foreign cooperatively-run institution under Chinese law'),
 (@u, 'HIGHLIGHT', 3,  'Instruction delivered in English following an American liberal-arts curriculum model'),
 (@u, 'HIGHLIGHT', 4,  'Formally established in 2014 after Ministry of Education approval in 2011'),
 (@u, 'HIGHLIGHT', 5,  'Purpose-built campus in Wenzhou, Zhejiang province'),
 (@u, 'HIGHLIGHT', 6,  'Dual degree-granting pathway recognized by both the Chinese and US higher-education systems'),
 (@u, 'HIGHLIGHT', 7,  'Faculty drawn from both Chinese and international academic backgrounds'),
 (@u, 'HIGHLIGHT', 8,  'Programs span business, science, engineering, humanities and communication'),
 (@u, 'HIGHLIGHT', 9,  'Growing full-time enrollment as the university expands toward its planned campus capacity'),
 (@u, 'HIGHLIGHT', 10, 'Located in Wenzhou, a major commercial city in Zhejiang province'),
 (@u, 'ADVANTAGE', 11, 'A US-style academic experience and degree pathway without the cost of full overseas study'),
 (@u, 'ADVANTAGE', 12, 'English-medium instruction eases the transition for international applicants'),
 (@u, 'ADVANTAGE', 13, 'Smaller class sizes than large Chinese public universities support closer student support'),
 (@u, 'ADVANTAGE', 14, 'Wenzhou offers a lower cost of living than China''s first-tier cities'),
 (@u, 'ADVANTAGE', 15, 'Joint governance with Kean University supports potential exchange and articulation opportunities'),
 (@u, 'ADVANTAGE', 16, 'A modern, single-purpose campus built for the cooperative program'),
 (@u, 'ADVANTAGE', 17, 'Wenzhou is well connected to Shanghai and Hangzhou by high-speed rail'),
 (@u, 'ADVANTAGE', 18, 'A curriculum structure designed with US accreditation standards in mind'),
 (@u, 'ADVANTAGE', 19, 'Business, engineering and communication programs aligned with international industry practice'),
 (@u, 'ADVANTAGE', 20, 'A distinct alternative for applicants who want a Western-style degree while remaining in China');
-- No ranking rows: Sino-foreign cooperative universities are generally excluded from mainstream domestic
-- ranking tables (CUAA, Wu Shulian, ShanghaiRanking BCUR). Add in dashboard if a credible source appears.

-- ============================================================================
-- 38. Wuhan University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Wuhan University', '武汉大学',
   'wuhan-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Wuhan', 'Hubei',
   1893, 59000, 3000, NULL, 'https://en.whu.edu.cn/', '985/211/Double First-Cl.',
   'Wuhan University is one of China''s most prestigious comprehensive research universities, directly administered by the Ministry of Education and consistently ranked among the top handful of universities nationally. It is a Project 985, Project 211 and Double First-Class institution offering programs across the sciences, engineering, medicine, law, humanities and management.',
   'The university traces its origins to the Ziqiang Institute founded in 1893 by Zhang Zhidong, the Qing-dynasty governor of Hubei and Hunan. It went through several reorganizations and name changes over the following century before taking its current form as a leading national comprehensive university.',
   'The main campus in Wuhan, Hubei, is known for its early-20th-century architecture on East Lake and its cherry blossom groves, alongside modern research and teaching facilities across multiple faculties.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Elite Project 985/211/Double First-Class institution -- highly selective; do not undersell its tier. Total/international student figures and ranking positions are approximate from public sources -- verify faculty count, current enrollment split and latest ranking editions before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Wuhan University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Project 985, Project 211 and Double First-Class national elite university'),
 (@u, 'HIGHLIGHT', 2,  'Traces its origins to the Ziqiang Institute, founded in 1893'),
 (@u, 'HIGHLIGHT', 3,  'Directly administered by the Ministry of Education'),
 (@u, 'HIGHLIGHT', 4,  'Consistently ranks among the top national universities in China'),
 (@u, 'HIGHLIGHT', 5,  'Comprehensive research university spanning sciences, engineering, medicine, law and humanities'),
 (@u, 'HIGHLIGHT', 6,  'Large-scale graduate education with strong doctoral and master''s programs'),
 (@u, 'HIGHLIGHT', 7,  'Campus known for historic architecture and its East Lake location'),
 (@u, 'HIGHLIGHT', 8,  'Hosts a significant international student population'),
 (@u, 'HIGHLIGHT', 9,  'Strong research output across natural sciences, engineering and social sciences'),
 (@u, 'HIGHLIGHT', 10, 'One of the largest and most selective universities in central China'),
 (@u, 'ADVANTAGE', 11, 'A globally recognized name that carries significant weight for graduate employment and further study'),
 (@u, 'ADVANTAGE', 12, 'Broad program choice across disciplines within a single elite institution'),
 (@u, 'ADVANTAGE', 13, 'Wuhan is a major transport and education hub in central China with lower living costs than coastal first-tier cities'),
 (@u, 'ADVANTAGE', 14, 'Established international student services and a track record of hosting foreign students'),
 (@u, 'ADVANTAGE', 15, 'Strong research supervision available for postgraduate applicants'),
 (@u, 'ADVANTAGE', 16, 'Scholarship pathways typical of Project 985 institutions for well-qualified international applicants'),
 (@u, 'ADVANTAGE', 17, 'Campus culture and facilities suited to a large, well-resourced comprehensive university'),
 (@u, 'ADVANTAGE', 18, 'High-speed rail connections put Wuhan within a few hours of Beijing, Shanghai and Guangzhou'),
 (@u, 'ADVANTAGE', 19, 'A recognized degree brand that supports competitive applications abroad after graduation'),
 (@u, 'ADVANTAGE', 20, 'Active alumni and industry networks across China given the university''s national standing');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 5, 2016, 'Older published position -- verify against current edition before publishing'),
 (@u, 'ShanghaiRanking BCUR', 8, 2024, 'Approximate band -- verify against current edition before publishing');

-- ============================================================================
-- 39. Wuxi University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Wuxi University', '无锡学院',
   'wuxi-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Wuxi', 'Jiangsu',
   2021, NULL, NULL, NULL, 'https://www.cwxu.edu.cn/', 'Public (newly indep.)',
   'Wuxi University is a public undergraduate university in Wuxi, Jiangsu, operated by the Wuxi municipal government and jointly supported by Nanjing University of Information Science and Technology (NUIST). It converted from an independent college to a stand-alone public institution in 2021.',
   'The university''s direct predecessor was the Nanjing University of Information Science and Technology Binjiang College, established in 2002 as an independent college, which relocated its campus to Wuxi in 2018. In February 2021 the Ministry of Education approved its conversion into Wuxi University, an independently established public undergraduate institution, with NUIST continuing as a supporting partner.',
   'The campus is located in Wuxi, Jiangsu, with facilities inherited and expanded from the former Binjiang College site, supported by ongoing academic cooperation with NUIST.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. CORRECTION vs. brief: verified sources (MOE announcement, Feb 2021) show the predecessor was Nanjing University of Information Science and Technology Binjiang College (est. 2002), NOT Jiangnan University Taihu College -- that separate lineage became Wuxi Taihu University instead. Written to verified facts. Total-students, international-students and faculty count not found -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Wuxi University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Newly independent public undergraduate university, established 2021'),
 (@u, 'HIGHLIGHT', 2,  'Converted from Nanjing University of Information Science and Technology Binjiang College'),
 (@u, 'HIGHLIGHT', 3,  'Operated by the Wuxi municipal government'),
 (@u, 'HIGHLIGHT', 4,  'Continuing academic support and cooperation from NUIST'),
 (@u, 'HIGHLIGHT', 5,  'Located in Wuxi, a major economic center in southern Jiangsu'),
 (@u, 'HIGHLIGHT', 6,  'Part of a national wave of independent colleges converting to public status'),
 (@u, 'HIGHLIGHT', 7,  'Undergraduate programs across engineering, science, business and information technology'),
 (@u, 'HIGHLIGHT', 8,  'Campus relocated to and expanded in Wuxi from 2018'),
 (@u, 'HIGHLIGHT', 9,  'Ministry-of-Education-approved public institution status'),
 (@u, 'HIGHLIGHT', 10, 'Growing institution positioned to serve Wuxi''s regional industry needs'),
 (@u, 'ADVANTAGE', 11, 'Public-university tuition levels following the 2021 conversion, more affordable than private-college fees'),
 (@u, 'ADVANTAGE', 12, 'Location in Wuxi places students close to the Shanghai-Suzhou-Wuxi industrial and tech corridor'),
 (@u, 'ADVANTAGE', 13, 'Continued NUIST academic ties support program quality during the university''s early independent years'),
 (@u, 'ADVANTAGE', 14, 'Wuxi offers strong internship and graduate-employment links with manufacturing and IT industries'),
 (@u, 'ADVANTAGE', 15, 'High-speed rail access to Shanghai, Nanjing and Suzhou'),
 (@u, 'ADVANTAGE', 16, 'A newer campus built out specifically for the institution''s public-university transition'),
 (@u, 'ADVANTAGE', 17, 'Lower cost of living relative to Shanghai while remaining within the same economic region'),
 (@u, 'ADVANTAGE', 18, 'Growing institutional investment as the municipal government builds out the university'),
 (@u, 'ADVANTAGE', 19, 'Manageable class sizes typical of a newly established public institution'),
 (@u, 'ADVANTAGE', 20, 'A practical, industry-oriented curriculum suited to applied engineering and business study');
-- No ranking rows: institution is not present in major ranking tables given its recent (2021) conversion
-- to independent public status. Add in dashboard if one appears.

-- ============================================================================
-- 40. Xi'an Shiyou University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Xi''an Shiyou University', '西安石油大学',
   'xian-shiyou-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Xi''an', 'Shaanxi',
   1951, 22000, NULL, NULL, 'https://www.xsyu.edu.cn/', 'Provincial undergrad',
   'Xi''an Shiyou University ("Shiyou" meaning petroleum) is a provincial public university in Xi''an, Shaanxi, and the only university in Northwest China specializing in petroleum and petrochemical engineering. It offers undergraduate and postgraduate programs centered on the oil and gas industry alongside broader engineering and management disciplines.',
   'The university was founded in 1951 as a petroleum-focused technical institution and has grown into a specialized public university jointly supported by the Shaanxi provincial government and China''s national energy sector, reflecting the region''s long history in oil and gas production.',
   'The campus is located in Xi''an, Shaanxi, with laboratories and research centers focused on petroleum engineering, geology and petrochemical processing, supporting close ties to national oil and gas enterprises.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Petroleum/energy-focused specialist university. Faculty count and international-student figures not found in public sources -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Xi''an Shiyou University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'The only university in Northwest China specializing in petroleum and petrochemical engineering'),
 (@u, 'HIGHLIGHT', 2,  'Founded in 1951'),
 (@u, 'HIGHLIGHT', 3,  'Provincial public university supported by Shaanxi and the national energy sector'),
 (@u, 'HIGHLIGHT', 4,  'Approximately 22,000 total students, including undergraduate and postgraduate levels'),
 (@u, 'HIGHLIGHT', 5,  'Close institutional links to China''s national oil and gas enterprises'),
 (@u, 'HIGHLIGHT', 6,  'Located in Xi''an, a major historical and educational city in Shaanxi'),
 (@u, 'HIGHLIGHT', 7,  'Specialized programs in petroleum engineering, geology and petrochemical processing'),
 (@u, 'HIGHLIGHT', 8,  'Also offers broader engineering, management and science disciplines'),
 (@u, 'HIGHLIGHT', 9,  'Long-standing reputation within China''s energy-industry education sector'),
 (@u, 'HIGHLIGHT', 10, 'Research facilities aligned with upstream and downstream oil and gas operations'),
 (@u, 'ADVANTAGE', 11, 'A focused, industry-relevant degree for students targeting the energy sector'),
 (@u, 'ADVANTAGE', 12, 'Direct pathways into China''s national oil and gas enterprises through university ties'),
 (@u, 'ADVANTAGE', 13, 'Xi''an offers a lower cost of living than China''s coastal first-tier cities'),
 (@u, 'ADVANTAGE', 14, 'A historically and culturally rich host city with strong domestic transport links'),
 (@u, 'ADVANTAGE', 15, 'Specialized laboratories and facilities not widely available at general comprehensive universities'),
 (@u, 'ADVANTAGE', 16, 'A recognized regional reputation in petroleum engineering education'),
 (@u, 'ADVANTAGE', 17, 'Manageable class sizes relative to China''s largest comprehensive universities'),
 (@u, 'ADVANTAGE', 18, 'Xi''an is a major air and high-speed rail hub for onward domestic travel'),
 (@u, 'ADVANTAGE', 19, 'A clear specialization that can strengthen applications for energy-sector graduate study abroad'),
 (@u, 'ADVANTAGE', 20, 'Provincial-government support helps keep tuition and living costs moderate');
-- No ranking rows: institution is not present in major national ranking tables reviewed. Add in
-- dashboard if a credible source appears.

-- ============================================================================
-- 41. Xidian University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Xidian University', '西安电子科技大学',
   'xidian-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Xi''an', 'Shaanxi',
   1931, 37886, NULL, NULL, 'https://www.xidian.edu.cn/', 'Double First-Class',
   'Xidian University, officially known in English as Xidian University (formerly Xi''an Electronic Science and Technology University), is a Project 211 and Double First-Class national university in Xi''an, Shaanxi, recognized as one of China''s leading institutions in electronics, telecommunications and information technology.',
   'The university traces its origins to 1931 and was reorganized several times through the 20th century under different names tied to China''s military and civilian electronics-education systems, before adopting the name Xidian University in 1988. It was designated a Project 211 key university in 1996.',
   'The campus is located in Xi''an, Shaanxi, with facilities concentrated on electronic engineering, computer science, communications and cybersecurity research.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Project 211/Double First-Class institution with strong national standing in electronics and IT -- reflect this tier accurately. Faculty count and international-student figures not found in public sources -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Xidian University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Project 211 and Double First-Class national university'),
 (@u, 'HIGHLIGHT', 2,  'Widely recognized as one of China''s leading universities in electronics and information technology'),
 (@u, 'HIGHLIGHT', 3,  'Traces its origins to 1931, adopting its current name in 1988'),
 (@u, 'HIGHLIGHT', 4,  'Designated a Project 211 key university in 1996'),
 (@u, 'HIGHLIGHT', 5,  'Approximately 37,900 total students across undergraduate and postgraduate levels'),
 (@u, 'HIGHLIGHT', 6,  'Strong research focus on electronic engineering, communications and computer science'),
 (@u, 'HIGHLIGHT', 7,  'Notable programs in cybersecurity and information technology'),
 (@u, 'HIGHLIGHT', 8,  'Ranked among the top Chinese universities for electronics-related disciplines'),
 (@u, 'HIGHLIGHT', 9,  'Located in Xi''an, a major technology and education hub in Northwest China'),
 (@u, 'HIGHLIGHT', 10, 'Close ties to China''s electronics, telecommunications and defense-adjacent industries'),
 (@u, 'ADVANTAGE', 11, 'A nationally recognized brand in electronics and IT that carries weight with employers'),
 (@u, 'ADVANTAGE', 12, 'Project 211 status supports access to stronger research funding and facilities'),
 (@u, 'ADVANTAGE', 13, 'Strong fit for applicants targeting careers in telecommunications, semiconductors or software'),
 (@u, 'ADVANTAGE', 14, 'Xi''an offers a lower cost of living than China''s coastal first-tier cities'),
 (@u, 'ADVANTAGE', 15, 'Established postgraduate research pathways in engineering and computer science'),
 (@u, 'ADVANTAGE', 16, 'Scholarship opportunities typical of Project 211 institutions for qualified applicants'),
 (@u, 'ADVANTAGE', 17, 'Industry partnerships with major Chinese technology and telecommunications firms'),
 (@u, 'ADVANTAGE', 18, 'A historically and culturally significant host city with good domestic transport links'),
 (@u, 'ADVANTAGE', 19, 'A degree brand recognized for competitive graduate study applications abroad in STEM fields'),
 (@u, 'ADVANTAGE', 20, 'A large, well-established campus community with strong alumni networks in the tech sector');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'ShanghaiRanking BCUR', 40, 2024, 'Approximate band -- verify against current edition before publishing');

-- ============================================================================
-- 42. Xuzhou Medical University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Xuzhou Medical University', '徐州医科大学',
   'xuzhou-medical-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Xuzhou', 'Jiangsu',
   1958, 13000, NULL, NULL, 'http://www.xzhmu.edu.cn/', 'Provincial undergrad',
   'Xuzhou Medical University is a provincial public medical university in Xuzhou, Jiangsu, offering undergraduate and postgraduate programs in clinical medicine, pharmacy, nursing, anesthesiology and related health sciences.',
   'The university originated in 1958 as the Xuzhou branch of Nanjing Medical College and developed into an independent institution over subsequent decades, taking the name Xuzhou Medical University in 2016 following approval to upgrade from college to university status.',
   'The campus is located in Xuzhou, Jiangsu, with affiliated teaching hospitals supporting clinical training across its medical, pharmacy and nursing programs.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Provincial medical university. Faculty count and international-student figures not found in public sources -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Xuzhou Medical University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public medical university in Xuzhou, Jiangsu'),
 (@u, 'HIGHLIGHT', 2,  'Originated in 1958 as the Xuzhou branch of Nanjing Medical College'),
 (@u, 'HIGHLIGHT', 3,  'Renamed Xuzhou Medical University in 2016 upon university-status approval'),
 (@u, 'HIGHLIGHT', 4,  'Over 13,000 students across medicine, pharmacy, nursing and related programs'),
 (@u, 'HIGHLIGHT', 5,  'Notable program strength in anesthesiology'),
 (@u, 'HIGHLIGHT', 6,  'Affiliated teaching hospitals supporting clinical training'),
 (@u, 'HIGHLIGHT', 7,  'Undergraduate and postgraduate degree offerings in the health sciences'),
 (@u, 'HIGHLIGHT', 8,  'Located in Xuzhou, a regional transport hub in northern Jiangsu'),
 (@u, 'HIGHLIGHT', 9,  'Focused specialization in clinical and pharmaceutical sciences'),
 (@u, 'HIGHLIGHT', 10, 'Part of Jiangsu''s network of provincial medical higher-education institutions'),
 (@u, 'ADVANTAGE', 11, 'Focused medical training with access to affiliated teaching hospitals'),
 (@u, 'ADVANTAGE', 12, 'Lower tuition and living costs typical of a provincial, non-first-tier-city location'),
 (@u, 'ADVANTAGE', 13, 'Xuzhou is well connected by high-speed rail to Shanghai, Nanjing and Beijing'),
 (@u, 'ADVANTAGE', 14, 'A recognized regional strength in anesthesiology and clinical medicine'),
 (@u, 'ADVANTAGE', 15, 'Manageable class sizes relative to China''s largest medical universities'),
 (@u, 'ADVANTAGE', 16, 'A clear specialization suited to applicants targeting medicine, pharmacy or nursing careers'),
 (@u, 'ADVANTAGE', 17, 'Clinical placement opportunities through affiliated hospital partnerships'),
 (@u, 'ADVANTAGE', 18, 'A stable, established provincial institution with a multi-decade track record'),
 (@u, 'ADVANTAGE', 19, 'Jiangsu''s broader healthcare and pharmaceutical industry base for internships and employment'),
 (@u, 'ADVANTAGE', 20, 'A more accessible admissions profile than China''s top-tier national medical schools');
-- No ranking rows: institution is not present in major national ranking tables reviewed. Add in
-- dashboard if a credible source appears.

-- ============================================================================
-- 43. Yangtze Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Yangtze Normal University', '长江师范学院',
   'yangtze-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Fuling', 'Chongqing',
   1931, 23000, NULL, NULL, 'https://www.yznu.edu.cn/', 'Provincial undergrad',
   'Yangtze Normal University is a public university in Fuling District, Chongqing, administered by the Chongqing municipal government. It offers a comprehensive range of undergraduate programs with a historic emphasis on teacher education.',
   'The institution''s roots trace back to the Fuling Official Academy of Classical Learning, founded in 1901, with its modern form dating to 1931. Fuling Normal College and Fuling Education Institute merged in 2001, and the institution was renamed Yangtze Normal University in 2006.',
   'The university operates two campuses, Jiangdong and Lidu, at the confluence of the Yangtze and Wu rivers in Fuling District, Chongqing.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Chongqing is a municipality, recorded in the province field per catalog convention. Faculty count and international-student figures not found in public sources -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Yangtze Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Public university administered by the Chongqing municipal government'),
 (@u, 'HIGHLIGHT', 2,  'Roots trace to the Fuling Official Academy of Classical Learning, founded in 1901'),
 (@u, 'HIGHLIGHT', 3,  'Modern institution dates to 1931; renamed Yangtze Normal University in 2006'),
 (@u, 'HIGHLIGHT', 4,  'More than 23,000 undergraduate students'),
 (@u, 'HIGHLIGHT', 5,  'Two campuses -- Jiangdong and Lidu -- at the confluence of the Yangtze and Wu rivers'),
 (@u, 'HIGHLIGHT', 6,  'Historic emphasis on teacher education alongside broader undergraduate disciplines'),
 (@u, 'HIGHLIGHT', 7,  'Located in Fuling District, Chongqing municipality'),
 (@u, 'HIGHLIGHT', 8,  'Campus area of roughly 1,700 acres'),
 (@u, 'HIGHLIGHT', 9,  'Comprehensive undergraduate program offerings across arts, science and education'),
 (@u, 'HIGHLIGHT', 10, 'Long institutional lineage spanning more than a century'),
 (@u, 'ADVANTAGE', 11, 'Affordable tuition and living costs typical of a Chongqing-district location'),
 (@u, 'ADVANTAGE', 12, 'Riverside campus setting at the Yangtze-Wu confluence'),
 (@u, 'ADVANTAGE', 13, 'Fuling is connected to central Chongqing by rail and road for wider city access'),
 (@u, 'ADVANTAGE', 14, 'A long-established teacher-education tradition for education-focused applicants'),
 (@u, 'ADVANTAGE', 15, 'A comprehensive undergraduate offering beyond education alone'),
 (@u, 'ADVANTAGE', 16, 'Chongqing''s status as a major inland economic center supports post-graduation opportunities'),
 (@u, 'ADVANTAGE', 17, 'Manageable class sizes relative to China''s largest comprehensive universities'),
 (@u, 'ADVANTAGE', 18, 'A lower-cost alternative to studying in Chongqing''s core urban districts'),
 (@u, 'ADVANTAGE', 19, 'Growing municipal investment in campus facilities and programs'),
 (@u, 'ADVANTAGE', 20, 'A stable, long-running institution with more than a century of institutional history');
-- No ranking rows: institution is not present in major national ranking tables reviewed. Add in
-- dashboard if a credible source appears.

-- ============================================================================
-- 44. Yanshan University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Yanshan University', '燕山大学',
   'yanshan-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Qinhuangdao', 'Hebei',
   1920, 40000, NULL, 2058, 'https://www.ysu.edu.cn/', 'Provincial engineering',
   'Yanshan University is a provincial public university in Qinhuangdao, Hebei, particularly noted for its programs in mechanical engineering and materials science. It offers a broad engineering-led curriculum alongside science, economics, management, law, arts and education.',
   'The university''s lineage traces to the Harbin Institute of Technology''s heavy-machinery programs, founded in 1920, which became the independent Northeast Heavy Machinery Institute in 1960. The institution relocated southward to Qinhuangdao starting in 1985, completing the move and adopting the name Yanshan University in 1997; Hebei provincial government took over its administration in 1998.',
   'The campus is located in the coastal city of Qinhuangdao, Hebei, with engineering laboratories and research centers supporting its mechanical engineering and materials science programs.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Faculty count of 2,058 refers to teaching staff per public sources; verify current figures along with international-student count before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Yanshan University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Notable national reputation in mechanical engineering and materials science'),
 (@u, 'HIGHLIGHT', 2,  'Lineage traces to Harbin Institute of Technology heavy-machinery programs, founded 1920'),
 (@u, 'HIGHLIGHT', 3,  'Became independent as Northeast Heavy Machinery Institute in 1960'),
 (@u, 'HIGHLIGHT', 4,  'Relocated to Qinhuangdao starting 1985, adopting the name Yanshan University in 1997'),
 (@u, 'HIGHLIGHT', 5,  'Provincial public university under Hebei government administration since 1998'),
 (@u, 'HIGHLIGHT', 6,  'Approximately 40,000 students as of 2023'),
 (@u, 'HIGHLIGHT', 7,  'Around 2,058 teaching staff, including hundreds of professors and associate professors'),
 (@u, 'HIGHLIGHT', 8,  '64 undergraduate majors spanning engineering, science, economics, management, law, art and education'),
 (@u, 'HIGHLIGHT', 9,  'Located in the coastal city of Qinhuangdao, Hebei'),
 (@u, 'HIGHLIGHT', 10, 'Engineering-led institution with strong industrial research ties'),
 (@u, 'ADVANTAGE', 11, 'A recognized specialization in mechanical engineering and materials science for applied-engineering applicants'),
 (@u, 'ADVANTAGE', 12, 'Qinhuangdao offers a coastal setting with lower living costs than Beijing or Tianjin'),
 (@u, 'ADVANTAGE', 13, 'Proximity to Beijing and Tianjin by rail for wider regional access'),
 (@u, 'ADVANTAGE', 14, 'A broad range of 64 undergraduate majors within a single university'),
 (@u, 'ADVANTAGE', 15, 'Industrial-heritage engineering programs with strong laboratory and research facilities'),
 (@u, 'ADVANTAGE', 16, 'A large, well-established student community supporting campus life and networking'),
 (@u, 'ADVANTAGE', 17, 'Hebei provincial support for tuition and facilities investment'),
 (@u, 'ADVANTAGE', 18, 'Strong ties to China''s heavy-machinery and manufacturing industries for internships'),
 (@u, 'ADVANTAGE', 19, 'A century-long institutional lineage rooted in a historically significant engineering school'),
 (@u, 'ADVANTAGE', 20, 'A relatively accessible admissions profile compared to China''s top-tier national universities');
-- No ranking rows: institution is not present in major national ranking tables reviewed. Add in
-- dashboard if a credible source appears.

-- ============================================================================
-- 45. Zhejiang University of Finance and Economics
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Zhejiang University of Finance and Economics', '浙江财经大学',
   'zhejiang-university-of-finance-and-economics', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Hangzhou', 'Zhejiang',
   1974, 24000, NULL, 910, 'https://www.zufe.edu.cn/', 'Provincial finance',
   'Zhejiang University of Finance and Economics is a provincial public university in Hangzhou, Zhejiang, specializing in economics, finance, law and management, with additional programs in the sciences and humanities.',
   'The university was founded in December 1974 as the Zhejiang Academy of Public Finance and Banking, later developing into a comprehensive finance-and-economics-focused university and taking its current name and university status over subsequent decades.',
   'The campus is located in Hangzhou, Zhejiang, a major financial and technology center, with facilities supporting its economics, finance, law and management programs.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. International-student figures not found in public sources -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Zhejiang University of Finance and Economics' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public university specializing in finance and economics'),
 (@u, 'HIGHLIGHT', 2,  'Founded in December 1974 as the Zhejiang Academy of Public Finance and Banking'),
 (@u, 'HIGHLIGHT', 3,  'Located in Hangzhou, a major Chinese financial and technology center'),
 (@u, 'HIGHLIGHT', 4,  'Approximately 24,000 undergraduate students'),
 (@u, 'HIGHLIGHT', 5,  'Around 910 full-time teaching staff'),
 (@u, 'HIGHLIGHT', 6,  'Programs concentrated in economics, finance, law and management'),
 (@u, 'HIGHLIGHT', 7,  'Additional offerings in science and humanities disciplines'),
 (@u, 'HIGHLIGHT', 8,  'Positioned within Zhejiang''s strong provincial finance-education sector'),
 (@u, 'HIGHLIGHT', 9,  'Half-century institutional history in economics and public-finance education'),
 (@u, 'HIGHLIGHT', 10, 'Located in one of China''s most economically dynamic provinces'),
 (@u, 'ADVANTAGE', 11, 'A finance-and-economics specialization well suited to business-career-focused applicants'),
 (@u, 'ADVANTAGE', 12, 'Hangzhou''s strong finance, e-commerce and technology industry base for internships and employment'),
 (@u, 'ADVANTAGE', 13, 'Proximity to Shanghai by high-speed rail for wider regional access'),
 (@u, 'ADVANTAGE', 14, 'A long institutional history in public finance and banking education'),
 (@u, 'ADVANTAGE', 15, 'Hangzhou offers a high quality of life relative to its economic development level'),
 (@u, 'ADVANTAGE', 16, 'Strong regional employer recognition within Zhejiang''s financial sector'),
 (@u, 'ADVANTAGE', 17, 'A focused curriculum that builds depth in finance, economics and law'),
 (@u, 'ADVANTAGE', 18, 'Access to Hangzhou''s dynamic private-sector and technology company ecosystem'),
 (@u, 'ADVANTAGE', 19, 'Manageable class sizes relative to China''s largest comprehensive universities'),
 (@u, 'ADVANTAGE', 20, 'Provincial government support for tuition affordability and campus investment');
-- No ranking rows: institution is not present in major national ranking tables reviewed. Add in
-- dashboard if a credible source appears.

-- ============================================================================
-- 46. Zhejiang Wanli University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Zhejiang Wanli University', '浙江万里学院',
   'zhejiang-wanli-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Ningbo', 'Zhejiang',
   1999, 20000, NULL, NULL, 'https://en.zwu.edu.cn/', 'Provincial undergrad',
   'Zhejiang Wanli University is a provincial public university in Ningbo, Zhejiang, offering a broad range of undergraduate programs across business, engineering, science, arts and law.',
   'The university''s predecessor institution, Ningbo Special District Agricultural College, was established in 1958. It was reorganized and founded as Zhejiang Wanli University in 1999 by the Wanli Education Group, later transitioning to its current public provincial status.',
   'The university spans three sites in Ningbo -- the Qianhu Campus, Huilong Campus and Zhashan Base -- covering roughly 95 hectares in total.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. International-student and faculty-count figures not found in public sources -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Zhejiang Wanli University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public university in Ningbo, Zhejiang'),
 (@u, 'HIGHLIGHT', 2,  'Predecessor institution, Ningbo Special District Agricultural College, established 1958'),
 (@u, 'HIGHLIGHT', 3,  'Founded under its current name in 1999'),
 (@u, 'HIGHLIGHT', 4,  'Approximately 20,000 students'),
 (@u, 'HIGHLIGHT', 5,  'Three campus sites -- Qianhu, Huilong and Zhashan -- totaling roughly 95 hectares'),
 (@u, 'HIGHLIGHT', 6,  'Broad undergraduate program offering across business, engineering, science, arts and law'),
 (@u, 'HIGHLIGHT', 7,  'Located in Ningbo, a major Chinese port and manufacturing city'),
 (@u, 'HIGHLIGHT', 8,  'Formerly known as the Ningbo Branch of Zhejiang Agricultural University'),
 (@u, 'HIGHLIGHT', 9,  'Provincial government administration supporting institutional stability'),
 (@u, 'HIGHLIGHT', 10, 'Established multi-decade presence in Ningbo higher education'),
 (@u, 'ADVANTAGE', 11, 'Affordable tuition and living costs typical of a provincial public university'),
 (@u, 'ADVANTAGE', 12, 'Ningbo''s strong manufacturing and port-logistics industry base for internships and employment'),
 (@u, 'ADVANTAGE', 13, 'Proximity to Shanghai and Hangzhou by high-speed rail'),
 (@u, 'ADVANTAGE', 14, 'A broad program catalog spanning business, engineering, science, arts and law'),
 (@u, 'ADVANTAGE', 15, 'Multiple campus sites offering varied facilities across disciplines'),
 (@u, 'ADVANTAGE', 16, 'Ningbo offers a coastal quality of life with a lower cost of living than Shanghai'),
 (@u, 'ADVANTAGE', 17, 'A stable public-university status supporting predictable tuition and governance'),
 (@u, 'ADVANTAGE', 18, 'Manageable class sizes relative to China''s largest comprehensive universities'),
 (@u, 'ADVANTAGE', 19, 'Growing institutional investment under provincial administration'),
 (@u, 'ADVANTAGE', 20, 'A practical, business- and engineering-oriented curriculum suited to applied study');
-- No ranking rows: institution is not present in major national ranking tables reviewed. Add in
-- dashboard if a credible source appears.

-- ============================================================================
-- 47. Zhongyuan University of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Zhongyuan University of Technology', '中原工学院',
   'zhongyuan-university-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Zhengzhou', 'Henan',
   1955, 22000, NULL, 1942, 'https://www.zut.edu.cn/', 'Provincial undergrad',
   'Zhongyuan University of Technology is a provincial public university in Zhengzhou, Henan, with historical strength in textile engineering that has broadened into a wider engineering, science, business and arts curriculum.',
   'The university was founded in 1955 as Zhengzhou Textile Institute, focused on textile engineering and related industries. It was renamed Zhongyuan University of Technology in July 2000 as its program offerings expanded beyond textiles into broader engineering and technology fields.',
   'The campus is located in Zhengzhou, Henan''s provincial capital, with facilities supporting its engineering, textile-technology, business and design programs.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. International-student figures not found in public sources -- verify before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Zhongyuan University of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public university in Zhengzhou, Henan'),
 (@u, 'HIGHLIGHT', 2,  'Founded in 1955 as Zhengzhou Textile Institute'),
 (@u, 'HIGHLIGHT', 3,  'Renamed Zhongyuan University of Technology in July 2000'),
 (@u, 'HIGHLIGHT', 4,  'Approximately 22,000 full-time students'),
 (@u, 'HIGHLIGHT', 5,  'Around 1,942 faculty members'),
 (@u, 'HIGHLIGHT', 6,  'Historical specialization in textile engineering now broadened to wider technology fields'),
 (@u, 'HIGHLIGHT', 7,  'Located in Zhengzhou, the provincial capital of Henan'),
 (@u, 'HIGHLIGHT', 8,  'Programs spanning engineering, science, business, design and the arts'),
 (@u, 'HIGHLIGHT', 9,  'Seven decades of institutional history in applied technology education'),
 (@u, 'HIGHLIGHT', 10, 'Positioned within Henan''s growing provincial higher-education network'),
 (@u, 'ADVANTAGE', 11, 'A recognized regional specialization in textile and applied engineering'),
 (@u, 'ADVANTAGE', 12, 'Zhengzhou offers a lower cost of living than China''s coastal first-tier cities'),
 (@u, 'ADVANTAGE', 13, 'Zhengzhou is a major national rail and logistics hub for onward domestic travel'),
 (@u, 'ADVANTAGE', 14, 'A broad program catalog spanning engineering, business, design and the arts'),
 (@u, 'ADVANTAGE', 15, 'Established industry ties in Henan''s manufacturing and textile sectors'),
 (@u, 'ADVANTAGE', 16, 'A stable, long-running public institution with more than seventy years of history'),
 (@u, 'ADVANTAGE', 17, 'Manageable class sizes relative to China''s largest comprehensive universities'),
 (@u, 'ADVANTAGE', 18, 'Provincial government support for tuition affordability and campus investment'),
 (@u, 'ADVANTAGE', 19, 'A practical, applied-technology curriculum suited to engineering-focused applicants'),
 (@u, 'ADVANTAGE', 20, 'A more accessible admissions profile than China''s top-tier national universities');
-- No ranking rows: institution is not present in major national ranking tables reviewed. Add in
-- dashboard if a credible source appears.

-- ============================================================================
-- 48. Zunyi Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Zunyi Normal University', '遵义师范学院',
   'zunyi-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Zunyi', 'Guizhou',
   1958, 6500, NULL, NULL, 'https://www.zync.edu.cn/', 'Provincial undergrad',
   'Zunyi Normal University is a provincial public university in Zunyi, Guizhou, with a historical focus on teacher education alongside a broader undergraduate curriculum.',
   'The institution was established as Zunyi Normal College in 1958 and was upgraded to Zunyi Normal University in 2001 with approval from the Ministry of Education.',
   'The campus is located in Zunyi, Guizhou, a city known for its historical significance, with facilities supporting teacher-education and general undergraduate programs.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-22. Total-student figure is a rough band from public sources (medium-sized institution) -- verify current headcount, faculty count and international-student figures before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Zunyi Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public university in Zunyi, Guizhou'),
 (@u, 'HIGHLIGHT', 2,  'Established as Zunyi Normal College in 1958'),
 (@u, 'HIGHLIGHT', 3,  'Upgraded to Zunyi Normal University in 2001 with Ministry of Education approval'),
 (@u, 'HIGHLIGHT', 4,  'Historical focus on teacher education'),
 (@u, 'HIGHLIGHT', 5,  'Medium-sized institution with several thousand enrolled students'),
 (@u, 'HIGHLIGHT', 6,  'Located in Zunyi, a historically significant city in Guizhou province'),
 (@u, 'HIGHLIGHT', 7,  'Broader undergraduate curriculum beyond teacher education'),
 (@u, 'HIGHLIGHT', 8,  'Recognized by the Guizhou Provincial Department of Education'),
 (@u, 'HIGHLIGHT', 9,  'Part of Guizhou''s provincial public higher-education network'),
 (@u, 'HIGHLIGHT', 10, 'Multi-decade institutional history dating to the late 1950s'),
 (@u, 'ADVANTAGE', 11, 'Affordable tuition and living costs typical of a Guizhou provincial university'),
 (@u, 'ADVANTAGE', 12, 'A long-established teacher-education tradition for education-focused applicants'),
 (@u, 'ADVANTAGE', 13, 'Zunyi is a culturally and historically significant city in southwestern China'),
 (@u, 'ADVANTAGE', 14, 'Smaller class sizes than China''s largest comprehensive universities'),
 (@u, 'ADVANTAGE', 15, 'A more accessible admissions profile than China''s top-tier national universities'),
 (@u, 'ADVANTAGE', 16, 'Lower cost of living in Guizhou relative to China''s coastal provinces'),
 (@u, 'ADVANTAGE', 17, 'Provincial government support for tuition affordability and campus investment'),
 (@u, 'ADVANTAGE', 18, 'A close-knit campus community typical of a medium-sized institution'),
 (@u, 'ADVANTAGE', 19, 'Growing regional development in Guizhou supporting local graduate opportunities'),
 (@u, 'ADVANTAGE', 20, 'A stable, long-running public institution with more than six decades of history');
-- No ranking rows: institution is not present in major national ranking tables reviewed. Add in
-- dashboard if a credible source appears.
