-- University catalog import: 22 institutions (all China) for partner catalog
-- build-out. DML only -- schema is V9 / V10 / V18 / V46 / V52.
--
-- Every row is inserted as status = ACTIVE, publish_status = DRAFT so nothing
-- reaches the public site until a staff member reviews it and publishes from the
-- admin Universities screen. partner_status = PROSPECT (INTERNAL / staff-only) --
-- these are prospective partners, not confirmed. reference_code continues the
-- NAD-UNI-NNNN sequence from whatever is already in nad_university.
--
-- Data provenance: public sources (Wikipedia, official university sites,
-- Baidu Baike) gathered 2026-09-10. Enrolment / faculty figures and every
-- ranking row are APPROXIMATE and carry a "verify" note -- confirm before
-- publishing. The following are deliberately left blank for the dashboard:
--   logo / banner / cover / gallery images, accommodation_info, nearby_info,
--   admissions_email, office_phone, academic departments (nad_department),
--   and most ranking rows. Highlights are a starter set to edit down / extend.
--
-- Manual rollback (children cascade via FK ON DELETE CASCADE):
--   delete from nad_university where create_by = 'import'
--     and reference_code like 'NAD-UNI-%';

set @rc := (select coalesce(max(cast(substring(reference_code, 9) as unsigned)), 0)
            from nad_university where reference_code like 'NAD-UNI-%');

-- ============================================================================
-- 1. Anhui Science and Technology University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Anhui Science and Technology University', '安徽科技学院',
   'anhui-science-and-technology-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Fengyang', 'Anhui',
   1950, 20000, NULL, 1200, 'https://www.ahstu.edu.cn/', 'Provincial undergrad',
   'Anhui Science and Technology University is a provincial public university in Fengyang County, Chuzhou, with additional campuses at Bengbu (Longhu) and Chuzhou. Rooted in agriculture and forestry, it has grown into a multi-disciplinary teaching university covering agriculture, engineering, science, management, economics, arts, law and medicine. It is known for applied agronomy, animal science, food science and mechanical engineering, and maintains close ties with the rural economy of the Huai River region. The university emphasises practice-oriented undergraduate training and technology transfer to local industry and farming co-operatives.',
   'The university traces its origins to the Northern Anhui Advanced Agriculture and Forestry School founded in 1950. Undergraduate education began in 1965. After several relocations and mergers it was based in Fengyang -- the birthplace of China''s rural household-responsibility reform -- and was renamed Anhui Science and Technology University in 2005 when it gained full university status. A second teaching base was later developed in Bengbu to broaden its engineering and management provision.',
   'The main campus sits in the county town of Fengyang, close to the Ming imperial tomb and city-wall heritage sites, with the newer Bengbu Longhu campus offering modern teaching and laboratory buildings. Facilities include agronomy experiment stations, greenhouses and demonstration farms, food-processing pilot lines and engineering workshops. Student accommodation is arranged in on-campus dormitory quarters.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10 from public sources for partner catalog build-out. Verify enrolment, faculty count and campus split (Fengyang / Bengbu / Chuzhou) before publishing. Partner status PROSPECT pending BD contact; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Anhui Science and Technology University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public university with a 70+ year record in agricultural and applied sciences'),
 (@u, 'HIGHLIGHT', 2,  'Three campuses: Fengyang (main), Bengbu Longhu and Chuzhou'),
 (@u, 'HIGHLIGHT', 3,  'Strong applied agronomy, animal science and food science and engineering programmes'),
 (@u, 'HIGHLIGHT', 4,  'Located in Fengyang, the historic starting point of China''s rural reform'),
 (@u, 'HIGHLIGHT', 5,  'Multi-disciplinary: agriculture, engineering, science, management, economics, arts, law, medicine'),
 (@u, 'HIGHLIGHT', 6,  'Demonstration farms, greenhouses and agronomy experiment stations for hands-on training'),
 (@u, 'HIGHLIGHT', 7,  'Food-processing pilot lines and engineering workshops on campus'),
 (@u, 'HIGHLIGHT', 8,  'Practice-oriented undergraduate curriculum with industry and co-operative placements'),
 (@u, 'HIGHLIGHT', 9,  'Active technology transfer to the rural economy of the Huai River region'),
 (@u, 'HIGHLIGHT', 10, 'Bachelor and selected master programmes with degree-granting authority'),
 (@u, 'ADVANTAGE', 11, 'Lower cost of living than provincial-capital universities'),
 (@u, 'ADVANTAGE', 12, 'Well suited to applicants targeting agri-food, agribusiness and rural development careers'),
 (@u, 'ADVANTAGE', 13, 'Modern teaching and laboratory buildings on the Bengbu Longhu campus'),
 (@u, 'ADVANTAGE', 14, 'Small county-town setting with heritage sites nearby (Ming tomb, city walls)'),
 (@u, 'ADVANTAGE', 15, 'Regional employer network across Anhui agriculture and manufacturing'),
 (@u, 'ADVANTAGE', 16, 'Established international student intake channel'),
 (@u, 'ADVANTAGE', 17, 'Rail links to Nanjing, Hefei and Bengbu from Fengyang / Bengbu'),
 (@u, 'ADVANTAGE', 18, 'Scholarship options for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 19, 'Supportive environment for Chinese-language study alongside a degree'),
 (@u, 'ADVANTAGE', 20, 'Responsive to partner-managed recruitment given its growth ambitions');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 330, 2024, 'Approximate band -- verify against current edition before publishing'),
 (@u, 'Wu Shulian / China Academic Degrees', 300, 2024, 'Approximate -- verify');

-- ============================================================================
-- 2. Beijing Forestry University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Beijing Forestry University', '北京林业大学',
   'beijing-forestry-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Beijing', 'Beijing',
   1952, 18400, NULL, 1079, 'https://www.bjfu.edu.cn/', 'Double First-Class',
   'Beijing Forestry University is a national key university in Haidian District, Beijing, directly under the Ministry of Education and part of Project 211 and the Double First-Class initiative. It is China''s leading institution for forestry, landscape architecture, ecology, and environmental science, and also teaches biology, economics, management, engineering, law and the humanities. Forestry, landscape architecture and forest engineering are among its nationally top-ranked disciplines. The university plays a central role in China''s afforestation, desertification-control and national-park research and has trained a large share of the country''s senior forestry scientists and administrators.',
   'The university''s lineage reaches back to the 1902 Forestry Section of the Imperial University of Peking. It became an independent institute in 1952 through the merger of the forestry programmes of Peking University and other institutions, and was designated a national key university. In 1985 it broadened beyond forestry into electronics, computing, economics and psychology and adopted its present name. It joined Project 211 in the 1990s and the Double First-Class list in 2017, with forestry and landscape architecture named world-class disciplines.',
   'The compact urban campus lies in Haidian, Beijing''s university and technology district, next to the Old Summer Palace and within reach of the Fragrant Hills. It houses botanical gardens, arboreta, herbaria, wind-tunnel and soil-and-water laboratories, and a museum of forest biodiversity. Students live in on-campus halls; the city''s subway network connects the campus to central Beijing.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10 from public sources. High-profile MoE university -- expect selective admission and competitive partnership terms. Verify current enrolment / faculty and Double First-Class discipline list. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Beijing Forestry University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'National key university under the Ministry of Education'),
 (@u, 'HIGHLIGHT', 2,  'Project 211 and Double First-Class institution'),
 (@u, 'HIGHLIGHT', 3,  'China''s top university for forestry and landscape architecture'),
 (@u, 'HIGHLIGHT', 4,  'Double First-Class disciplines: forestry and landscape architecture'),
 (@u, 'HIGHLIGHT', 5,  'Strong ecology, environmental science, wildlife and soil-and-water conservation programmes'),
 (@u, 'HIGHLIGHT', 6,  'Located in Haidian, Beijing''s university and R&D district'),
 (@u, 'HIGHLIGHT', 7,  'Roots dating to the 1902 forestry section of the Imperial University of Peking'),
 (@u, 'HIGHLIGHT', 8,  '14 schools, 60+ undergraduate and 120+ postgraduate programmes'),
 (@u, 'HIGHLIGHT', 9,  'Faculty includes academicians of the Chinese Academy of Engineering'),
 (@u, 'HIGHLIGHT', 10, 'Botanical gardens, arboreta, herbaria and a forest-biodiversity museum on campus'),
 (@u, 'ADVANTAGE', 11, 'Central role in national afforestation, desertification-control and national-park research'),
 (@u, 'ADVANTAGE', 12, 'Partnerships with 150+ universities and institutes across 20+ countries'),
 (@u, 'ADVANTAGE', 13, 'Beijing location gives access to ministries, research academies and industry'),
 (@u, 'ADVANTAGE', 14, 'Well-connected by subway to central Beijing'),
 (@u, 'ADVANTAGE', 15, 'Strong graduate-employment record in government, academia and green industry'),
 (@u, 'ADVANTAGE', 16, 'English-taught graduate programmes in ecology and environmental fields (verify current list)'),
 (@u, 'ADVANTAGE', 17, 'Chinese Government / university scholarships available for international students (verify)'),
 (@u, 'ADVANTAGE', 18, 'Attractive for applicants targeting climate, conservation and sustainable-land-use careers'),
 (@u, 'ADVANTAGE', 19, 'Alumni network of 30,000+ graduates including 11 academicians'),
 (@u, 'ADVANTAGE', 20, 'Recognised international brand in its specialist fields');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'QS World University Rankings', 851, 2026, 'Approximate band (851-900) -- verify current edition'),
 (@u, 'THE World University Rankings', 601, 2026, 'Approximate band (601-800) -- verify'),
 (@u, 'ARWU / Shanghai Ranking', 601, 2025, 'Approximate band (601-700) -- verify'),
 (@u, 'US News Best Global Universities', 740, 2025, 'Approximate -- verify'),
 (@u, 'CUAA China (Alumni Association)', 62, 2024, 'Approximate -- verify'),
 (@u, 'ARWU subject -- Forestry', 20, 2024, 'Top-tier in forestry subject -- verify exact position');

-- ============================================================================
-- 3. Beijing Institute of Technology, Zhuhai
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Beijing Institute of Technology, Zhuhai', '北京理工大学珠海学院',
   'beijing-institute-of-technology-zhuhai', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PRIVATE', 'Zhuhai', 'Guangdong',
   2004, 20000, NULL, NULL, 'https://www.bitzh.edu.cn/', 'Private undergraduate',
   'Beijing Institute of Technology, Zhuhai (BITZH) is a private undergraduate institution in Tangjiawan, Zhuhai, Guangdong, approved by the Ministry of Education in 2004. It was founded jointly by Beijing Institute of Technology and a private partner and is supervised by the Guangdong Department of Education. Teaching focuses on engineering, information technology, design, international business, management and foreign languages, with an applied, industry-facing orientation. It is a national training base for international-business talent and a member of the CDIO engineering-education alliance. Its Greater Bay Area location gives students strong exposure to manufacturing, electronics and cross-border trade employers.',
   'BITZH was established in 2004 as an "independent college" linked to Beijing Institute of Technology, part of a wave of public-private undergraduate colleges created in China in the early 2000s to expand access. It built a large modern campus in Zhuhai''s Tangjiawan university district. National policy has since pushed such colleges toward full independence; the institution''s registration status and possible transition should be confirmed directly.',
   'The Zhuhai campus is large and landscaped, set against hills near the coast in the Tangjiawan higher-education cluster alongside branch campuses of other major universities. Facilities include engineering and design studios, IT and electronics laboratories, sports grounds and extensive on-campus student accommodation. Zhuhai is adjacent to Macau and a short sea or road link from Shenzhen and Hong Kong.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Private / former "independent college" -- CONFIRM current registration name and status (national policy has been converting these). Verify enrolment, fees and whether it still carries the BIT name. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Beijing Institute of Technology, Zhuhai' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Private undergraduate institution in the Greater Bay Area'),
 (@u, 'HIGHLIGHT', 2,  'Founded 2004 with Ministry of Education approval, linked to Beijing Institute of Technology'),
 (@u, 'HIGHLIGHT', 3,  'Located in Tangjiawan, Zhuhai''s higher-education district'),
 (@u, 'HIGHLIGHT', 4,  'Applied focus: engineering, IT, design, international business, languages'),
 (@u, 'HIGHLIGHT', 5,  'National training base for international-business talent'),
 (@u, 'HIGHLIGHT', 6,  'Member of the CDIO engineering-education alliance'),
 (@u, 'HIGHLIGHT', 7,  'Large modern landscaped campus with extensive facilities'),
 (@u, 'HIGHLIGHT', 8,  'Engineering, design and electronics laboratories and studios'),
 (@u, 'HIGHLIGHT', 9,  'Ample on-campus student accommodation'),
 (@u, 'HIGHLIGHT', 10, 'Adjacent to Macau; close links to Shenzhen and Hong Kong'),
 (@u, 'ADVANTAGE', 11, 'Strong regional employer base in manufacturing, electronics and cross-border trade'),
 (@u, 'ADVANTAGE', 12, 'Industry-facing, practice-oriented curriculum'),
 (@u, 'ADVANTAGE', 13, 'Milder subtropical climate and coastal setting'),
 (@u, 'ADVANTAGE', 14, 'More accessible admission than flagship public universities'),
 (@u, 'ADVANTAGE', 15, 'Good fit for applicants prioritising employability and Bay Area exposure'),
 (@u, 'ADVANTAGE', 16, 'International-business and languages programmes with cross-border orientation'),
 (@u, 'ADVANTAGE', 17, 'Campus life amenities: sports grounds, clubs, student services'),
 (@u, 'ADVANTAGE', 18, 'Potential articulation / exchange links (verify current agreements)'),
 (@u, 'ADVANTAGE', 19, 'Responsive to partner-managed international recruitment'),
 (@u, 'ADVANTAGE', 20, 'Zhuhai transport: bridge and ferry links across the Pearl River Delta');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'Wu Shulian -- private colleges', 10, 2024, 'Approximate top band among private colleges -- verify'),
 (@u, 'CUAA China Private Universities', 12, 2024, 'Approximate -- verify');

-- ============================================================================
-- 4. Beijing Institute of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Beijing Institute of Technology', '北京理工大学',
   'beijing-institute-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Beijing', 'Beijing',
   1940, 26000, NULL, 1953, 'https://www.bit.edu.cn/', 'Double First-Class',
   'Beijing Institute of Technology (BIT) is a top-tier national research university in Beijing, supervised by the Ministry of Industry and Information Technology and included in Project 985, Project 211 and the Double First-Class initiative. It is one of China''s strongest science-and-engineering universities, historically distinguished in defence technology, mechatronics, aerospace, materials, vehicle engineering, radar and information science, alongside growing programmes in management, design and the social sciences. BIT operates campuses in Beijing (Zhongguancun and Liangxiang) and a newer campus in Zhuhai, and maintains extensive international collaboration.',
   'BIT originates from the Yan''an Academy of Natural Sciences, founded in 1940 -- the first science-and-engineering university established by the Chinese Communist Party. The institution moved several times during the 1940s and was reconstituted in Beijing, becoming Beijing Institute of Technology in 1952. It was among the first universities placed on the national key list, entered Project 211 and Project 985 in the 1990s, and joined the Double First-Class list in 2017.',
   'The historic Zhongguancun campus sits in Haidian among China''s leading universities and technology firms, with the larger Liangxiang campus in Fangshan District hosting most undergraduate teaching and research institutes. Facilities include national key laboratories, engineering test halls, wind tunnels and a supercomputing centre. Students live in on-campus halls; Beijing''s subway serves both principal campuses.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Elite 985 university with defence-linked heritage -- some programmes may be restricted for international applicants; confirm eligibility by field and nationality. Verify enrolment / faculty and campus allocation (Beijing vs Zhuhai). Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Beijing Institute of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Project 985, Project 211 and Double First-Class national research university'),
 (@u, 'HIGHLIGHT', 2,  'One of China''s leading science-and-engineering universities'),
 (@u, 'HIGHLIGHT', 3,  'Founded 1940 as the first CCP-established science-and-engineering university'),
 (@u, 'HIGHLIGHT', 4,  'Historic strengths in mechatronics, aerospace, vehicle engineering, radar and materials'),
 (@u, 'HIGHLIGHT', 5,  'Campuses in Beijing (Zhongguancun, Liangxiang) and Zhuhai'),
 (@u, 'HIGHLIGHT', 6,  '16 national key disciplines; 60 bachelor, 144 master, 62 doctoral programmes'),
 (@u, 'HIGHLIGHT', 7,  'National key laboratories, engineering test halls and a supercomputing centre'),
 (@u, 'HIGHLIGHT', 8,  'QS World top ~300; ARWU world 101-150 (verify current)'),
 (@u, 'HIGHLIGHT', 9,  'Zhongguancun location amid China''s top universities and tech industry'),
 (@u, 'HIGHLIGHT', 10, 'Faculty includes academicians of the Chinese Academies of Science and Engineering'),
 (@u, 'ADVANTAGE', 11, 'Strong graduate outcomes in aerospace, automotive, defence and IT sectors'),
 (@u, 'ADVANTAGE', 12, 'Wide international partnership and joint-programme network'),
 (@u, 'ADVANTAGE', 13, 'English-taught degree programmes for international students (verify list)'),
 (@u, 'ADVANTAGE', 14, 'Chinese Government Scholarship and BIT scholarships available (verify terms)'),
 (@u, 'ADVANTAGE', 15, 'Well served by Beijing subway on both main campuses'),
 (@u, 'ADVANTAGE', 16, 'Prestige and recognisability for applicants and employers'),
 (@u, 'ADVANTAGE', 17, 'Comprehensive campus facilities: libraries, sports, student services'),
 (@u, 'ADVANTAGE', 18, 'Active student research and international competition record'),
 (@u, 'ADVANTAGE', 19, 'Zhuhai campus offers a Greater Bay Area option under the same brand'),
 (@u, 'ADVANTAGE', 20, 'Strong alumni presence across Chinese high-tech industry and government');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'QS World University Rankings', 302, 2026, 'Per ~2026 edition -- re-check yearly'),
 (@u, 'THE World University Rankings', 201, 2026, 'Approximate band (201-250) -- verify'),
 (@u, 'ARWU / Shanghai Ranking', 101, 2025, 'Approximate band (101-150) -- verify'),
 (@u, 'US News Best Global Universities', 179, 2025, 'Approximate -- verify'),
 (@u, 'QS China (Mainland)', 15, 2025, 'Approximate -- verify'),
 (@u, 'Best Chinese Universities Ranking', 16, 2024, 'Approximate -- verify');

-- ============================================================================
-- 5. Changchun Institute of Technology
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Changchun Institute of Technology', '长春工程学院',
   'changchun-institute-of-technology', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Changchun', 'Jilin',
   1951, NULL, NULL, NULL, 'https://www.ccit.edu.cn/', 'Provincial undergrad',
   'Changchun Institute of Technology is a provincial public undergraduate college in Changchun, Jilin, affiliated with the Jilin Provincial Government and authorised to grant bachelor and selected master degrees. It is an engineering-focused institution built around electrical engineering and automation, civil engineering, water resources and hydropower, surveying and resource exploration, energy and geomatics, with supporting programmes in management, business and the humanities. Its curriculum is applied and closely tied to the power, construction and water-conservancy industries of northeast China. The college enrols international students from more than a dozen countries.',
   'The institution was formed in 2000 by merging three long-standing specialised colleges in Changchun -- in electric power, construction/architecture and geology/surveying -- whose individual histories run back to 1951. It was approved as a full undergraduate institute and has since expanded its campus and gained master-level programmes in several engineering fields.',
   'The campus is in Changchun, the capital of Jilin and a centre of China''s automotive and rail-equipment industry. It provides engineering laboratories and workshops for power systems, structural testing, hydraulics and surveying, plus libraries, sports facilities and on-campus dormitories. Changchun has a continental climate with cold winters and is served by high-speed rail and an international airport.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10 from public sources -- thin English data. Verify enrolment, faculty count, master-programme list and international-student provision before publishing. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Changchun Institute of Technology' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public engineering college in Changchun, Jilin'),
 (@u, 'HIGHLIGHT', 2,  'Authorised to award bachelor and selected master degrees'),
 (@u, 'HIGHLIGHT', 3,  'Formed in 2000 from three specialised colleges dating to 1951'),
 (@u, 'HIGHLIGHT', 4,  'Core strengths: electrical engineering and automation, civil engineering'),
 (@u, 'HIGHLIGHT', 5,  'Water resources, hydropower and surveying / geomatics programmes'),
 (@u, 'HIGHLIGHT', 6,  '20 schools and around 54 undergraduate programmes'),
 (@u, 'HIGHLIGHT', 7,  'Applied curriculum tied to power, construction and water-conservancy industries'),
 (@u, 'HIGHLIGHT', 8,  'Engineering laboratories for power systems, structural testing and hydraulics'),
 (@u, 'HIGHLIGHT', 9,  'International students enrolled from 13+ countries'),
 (@u, 'HIGHLIGHT', 10, 'Located in a major automotive and rail-equipment manufacturing city'),
 (@u, 'ADVANTAGE', 11, 'Clear employment pathways into energy, infrastructure and surveying sectors'),
 (@u, 'ADVANTAGE', 12, 'Lower tuition and living costs typical of Jilin institutions'),
 (@u, 'ADVANTAGE', 13, 'High-speed rail and international airport in Changchun'),
 (@u, 'ADVANTAGE', 14, 'Accessible admission for applied-engineering applicants'),
 (@u, 'ADVANTAGE', 15, 'On-campus dormitories and student facilities'),
 (@u, 'ADVANTAGE', 16, 'Good option for Chinese-language study alongside an engineering degree'),
 (@u, 'ADVANTAGE', 17, 'Regional employer links across northeast China industry'),
 (@u, 'ADVANTAGE', 18, 'Practical workshop and field-training component in most programmes'),
 (@u, 'ADVANTAGE', 19, 'Provincial-government backing for continued development'),
 (@u, 'ADVANTAGE', 20, 'Open to partner-managed international recruitment');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 360, 2024, 'Approximate band -- verify'),
 (@u, 'Wu Shulian / China Academic Degrees', 340, 2024, 'Approximate -- verify');

-- ============================================================================
-- 6. Chongqing University of Chinese Medicine
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Chongqing University of Chinese Medicine', '重庆中医药学院',
   'chongqing-university-of-chinese-medicine', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Chongqing', 'Chongqing',
   2023, NULL, NULL, NULL, NULL, 'New university (2023)',
   'Chongqing University of Chinese Medicine is a new public university in Chongqing, approved by the Ministry of Education in 2023 to consolidate the municipality''s traditional Chinese medicine (TCM) teaching, hospital and research capacity into a single independent institution. It brings together the former College of Traditional Chinese Medicine of Chongqing Medical University, the Chongqing Traditional Chinese Medicine Hospital, and municipal academies for Chinese materia medica and medicinal-plant cultivation. Programmes centre on Chinese medicine, integrated Chinese and Western medicine, acupuncture and tuina, pharmacy of Chinese materia medica, and nursing.',
   'The institution''s roots lie in the Chongqing Traditional Chinese Medicine Advanced Studies School founded in 1951. In 2001 the municipal TCM school was absorbed into Chongqing Medical University as its College of Traditional Chinese Medicine. In June 2023 the Ministry of Education approved the establishment of an independent Chongqing University of Chinese Medicine by merging that college with affiliated TCM hospitals and research academies.',
   'The university is establishing its campus in Chongqing, a major mountainous municipality in southwest China, with affiliated teaching hospitals providing clinical training. As a newly formed institution its permanent campus, facilities and student capacity are still being built out and should be confirmed directly.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. VERY NEW (established 2023) -- almost no stable public data. Confirm official name (Chinese medicine "学院" vs "大学"), website, campus, capacity and whether it admits international students at all. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Chongqing University of Chinese Medicine' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'New public university established in 2023 with Ministry of Education approval'),
 (@u, 'HIGHLIGHT', 2,  'Consolidates Chongqing''s TCM teaching, hospital and research capacity'),
 (@u, 'HIGHLIGHT', 3,  'Formed from Chongqing Medical University''s College of Traditional Chinese Medicine'),
 (@u, 'HIGHLIGHT', 4,  'Incorporates the Chongqing Traditional Chinese Medicine Hospital'),
 (@u, 'HIGHLIGHT', 5,  'Includes municipal academies for Chinese materia medica and medicinal-plant cultivation'),
 (@u, 'HIGHLIGHT', 6,  'Heritage dating to a 1951 TCM advanced-studies school'),
 (@u, 'HIGHLIGHT', 7,  'Focus on Chinese medicine, acupuncture and tuina, integrated medicine'),
 (@u, 'HIGHLIGHT', 8,  'Pharmacy of Chinese materia medica and nursing programmes'),
 (@u, 'HIGHLIGHT', 9,  'Affiliated teaching hospitals for clinical training'),
 (@u, 'HIGHLIGHT', 10, 'Located in Chongqing, a major municipality in southwest China'),
 (@u, 'ADVANTAGE', 11, 'Dedicated single-discipline focus on TCM at university level'),
 (@u, 'ADVANTAGE', 12, 'Strong clinical network through merged hospitals and academies'),
 (@u, 'ADVANTAGE', 13, 'Potential appeal to international applicants interested in TCM'),
 (@u, 'ADVANTAGE', 14, 'Municipal government backing as a flagship new institution'),
 (@u, 'ADVANTAGE', 15, 'Chongqing is well connected by air and high-speed rail'),
 (@u, 'ADVANTAGE', 16, 'Opportunity to partner early with a growing institution'),
 (@u, 'ADVANTAGE', 17, 'Access to southwest China''s medicinal-plant resources for research'),
 (@u, 'ADVANTAGE', 18, 'Combined Chinese and Western medicine training model'),
 (@u, 'ADVANTAGE', 19, 'New facilities under construction'),
 (@u, 'ADVANTAGE', 20, 'Room to shape recruitment agreements as programmes mature');
-- No ranking rows: institution is too new to be ranked. Add in dashboard once published lists appear.

-- ============================================================================
-- 7. East China University of Political Science and Law
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('East China University of Political Science and Law', '华东政法大学',
   'east-china-university-of-political-science-and-law', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Shanghai', 'Shanghai',
   1952, 15400, NULL, 1400, 'https://www.ecupl.edu.cn/', 'Provincial key',
   'East China University of Political Science and Law (ECUPL) is a leading specialist law university in Shanghai, among the first group of political-science-and-law institutions founded in the People''s Republic. It is one of China''s most influential law schools, with nationally top-ranked programmes in law, and additional strengths in political science, economics, management, journalism and foreign languages applied to legal practice. ECUPL is a major supplier of judges, prosecutors, lawyers and legal scholars in Shanghai and East China, and runs an extensive network of international partnerships and joint legal-studies programmes.',
   'ECUPL was founded in 1952 by merging the law and political-science faculties of several universities in East China, on the former campus of St. John''s University in Shanghai. It was suspended and re-established twice during political campaigns before resuming permanently in 1979. It adopted university status and its current English name in 2007 and has since expanded to a second campus in Songjiang while keeping its historic Changning site.',
   'The university has two campuses: the historic Changning (Wanhangdu) campus beside Suzhou Creek, with early-20th-century academic buildings from the former St. John''s University, and the larger modern Songjiang campus in Shanghai''s university town. Facilities include moot courtrooms, legal-aid clinics, a large law library and specialist research centres. Both campuses have student accommodation and are connected to central Shanghai by metro.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Specialist law university -- programme fit is narrow (law, politics, related fields). Verify enrolment / faculty and current international joint-programme list. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'East China University of Political Science and Law' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'One of China''s leading specialist law universities'),
 (@u, 'HIGHLIGHT', 2,  'Among the first political-science-and-law institutions of the People''s Republic (1952)'),
 (@u, 'HIGHLIGHT', 3,  'Nationally top-ranked law programmes'),
 (@u, 'HIGHLIGHT', 4,  'Located in Shanghai, China''s commercial and legal-services hub'),
 (@u, 'HIGHLIGHT', 5,  'Historic Changning campus on the former St. John''s University site'),
 (@u, 'HIGHLIGHT', 6,  'Modern Songjiang campus in Shanghai''s university town'),
 (@u, 'HIGHLIGHT', 7,  'Moot courtrooms, legal-aid clinics and a major law library'),
 (@u, 'HIGHLIGHT', 8,  '30+ research centres and four academic law journals'),
 (@u, 'HIGHLIGHT', 9,  'Additional programmes in political science, economics, management and journalism'),
 (@u, 'HIGHLIGHT', 10, 'Major supplier of judges, prosecutors, lawyers and legal scholars in East China'),
 (@u, 'ADVANTAGE', 11, '~214 partnership agreements across 48 countries and regions'),
 (@u, 'ADVANTAGE', 12, 'Joint and dual-degree legal-studies programmes with overseas universities'),
 (@u, 'ADVANTAGE', 13, 'Strong Shanghai employer and internship network in law and finance'),
 (@u, 'ADVANTAGE', 14, 'Metro access to central Shanghai from both campuses'),
 (@u, 'ADVANTAGE', 15, 'Chinese-law and comparative-law courses for international students (verify)'),
 (@u, 'ADVANTAGE', 16, 'Scholarships for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 17, 'Prestigious brand for applicants targeting Chinese legal practice or China-related law'),
 (@u, 'ADVANTAGE', 18, 'LLM / Chinese Business Law English-taught options (verify)'),
 (@u, 'ADVANTAGE', 19, 'Active international moot-court and exchange participation'),
 (@u, 'ADVANTAGE', 20, 'Well-regarded alumni network in judiciary, government and corporate law');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 95, 2024, 'Approximate -- verify'),
 (@u, 'ARWU / Best Chinese Universities', 110, 2024, 'Approximate -- verify'),
 (@u, 'China Law School rankings (national)', 4, 2024, 'Approximate top-5 in law -- verify'),
 (@u, 'QS World subject -- Law', 251, 2025, 'Appears in some editions (band 251-300) -- verify');

-- ============================================================================
-- 8. Hailing International School
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hailing International School', NULL,
   'hailing-international-school', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PRIVATE', NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL,
   'Hailing International School is listed here as provided for the catalog import. It appears to be a private international / K-12 school in China rather than a higher-education institution, so most higher-education fields (rankings, faculty, degree programmes) do not apply. The exact identity should be confirmed: candidates include a school in the Hailing district of Taizhou (Jiangsu) and the "Hailiang" international school group in Zhejiang, which run integrated East-West curricula and host international students, including fully funded Belt and Road programme students from Southeast Asia.',
   'History unknown from public sources -- confirm founding year, operator and campus with the school directly.',
   'Campus details unknown -- confirm location, boarding provision and facilities directly. If this record is meant to be a university, replace it with the correct institution.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. NEEDS CLARIFICATION -- name is ambiguous and this looks like a K-12 / international school, not a university. Likely "Hailiang" (Zhejiang) or a school in Hailing district, Taizhou (Jiangsu). Confirm identity, decide whether it belongs in the university catalog, and set name_cn / city / province / type accordingly. Partner status PROSPECT.',
   'import', now());
set @u := (select id from nad_university where name = 'Hailing International School' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provided in the import list -- identity to be confirmed'),
 (@u, 'HIGHLIGHT', 2,  'Appears to be a private international / K-12 school rather than a university'),
 (@u, 'HIGHLIGHT', 3,  'Likely runs an integrated Chinese and Western curriculum'),
 (@u, 'HIGHLIGHT', 4,  'Reported to host international students, including funded Belt and Road students'),
 (@u, 'HIGHLIGHT', 5,  'Boarding provision with student mentoring reported for similar schools in the group'),
 (@u, 'ADVANTAGE', 6,  'Confirm whether this record should stay in the university catalog'),
 (@u, 'ADVANTAGE', 7,  'If retained, set city, province, founding year and Chinese name from the school'),
 (@u, 'ADVANTAGE', 8,  'May be relevant for pathway / foundation-year student flows'),
 (@u, 'ADVANTAGE', 9,  'Private operator -- likely flexible on partner agreements'),
 (@u, 'ADVANTAGE', 10, 'Placeholder record: enrich or replace from the dashboard');
-- No ranking rows: not a ranked higher-education institution.

-- ============================================================================
-- 9. Hainan University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hainan University', '海南大学',
   'hainan-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Haikou', 'Hainan',
   1958, 46600, NULL, NULL, 'https://www.hainanu.edu.cn/', 'Double First-Class',
   'Hainan University is the flagship comprehensive university of Hainan Province, based in Haikou, and is part of Project 211 and the Double First-Class initiative. Jointly administered by the Ministry of Education and the Hainan provincial government, it covers philosophy, economics, law, education, literature, science, engineering, agriculture, medicine, management, and the arts. It has particular strengths in tropical agriculture, crop science, marine and South China Sea resource research, food science, law, and civil and environmental engineering, and is positioned as a talent and innovation hub for the Hainan Free Trade Port.',
   'The present university was formed on 14 August 2007 when the Ministry of Education approved the merger of the original Hainan University (founded 1958) with the South China University of Tropical Agriculture, whose lineage runs to the South China Institute of Tropical Crops established in 1954 to develop natural-rubber production. Hainan University entered Project 211 in 2008, was added to the Double First-Class list, and in 2018 came under joint ministry-provincial administration to support the Free Trade Port.',
   'The university operates five campuses across Haikou, Danzhou and Sanya. The main Haikou campuses front the sea near the city centre; the Danzhou campus continues the tropical-agriculture mission with plantations and field stations, and a Sanya presence supports deep-sea and Nanhai research. Facilities include a State Key Laboratory for Marine Resource Utilization in the South China Sea. Students live in on-campus halls; Haikou has an international airport and high-speed rail around the island.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Strategic FTP-linked 211 university -- likely receptive to international partnerships. Verify enrolment split across the five campuses, faculty count and current Double First-Class discipline. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Hainan University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Flagship comprehensive university of Hainan Province'),
 (@u, 'HIGHLIGHT', 2,  'Project 211 and Double First-Class institution'),
 (@u, 'HIGHLIGHT', 3,  'Jointly run by the Ministry of Education and the Hainan government'),
 (@u, 'HIGHLIGHT', 4,  'Talent and innovation hub for the Hainan Free Trade Port'),
 (@u, 'HIGHLIGHT', 5,  'Leading tropical-agriculture and crop-science programmes'),
 (@u, 'HIGHLIGHT', 6,  'State Key Laboratory of Marine Resource Utilization in the South China Sea'),
 (@u, 'HIGHLIGHT', 7,  'Five campuses across Haikou, Danzhou and Sanya'),
 (@u, 'HIGHLIGHT', 8,  '~46,000 students across all levels'),
 (@u, 'HIGHLIGHT', 9,  'Full disciplinary spread from agriculture and engineering to law and the arts'),
 (@u, 'HIGHLIGHT', 10, 'Formed by the 2007 merger of Hainan University and South China University of Tropical Agriculture'),
 (@u, 'ADVANTAGE', 11, 'Island setting with a tropical climate and coastal campuses'),
 (@u, 'ADVANTAGE', 12, 'Free Trade Port policies bring investment, internships and international exposure'),
 (@u, 'ADVANTAGE', 13, 'Strong fit for applicants in tropical agriculture, marine science, tourism and trade'),
 (@u, 'ADVANTAGE', 14, 'Chinese Government and provincial scholarships for international students (verify)'),
 (@u, 'ADVANTAGE', 15, 'English-taught programmes in selected fields (verify current list)'),
 (@u, 'ADVANTAGE', 16, 'Haikou international airport and island-wide high-speed rail'),
 (@u, 'ADVANTAGE', 17, 'Growing international student community'),
 (@u, 'ADVANTAGE', 18, 'Field stations and plantations for applied agricultural training'),
 (@u, 'ADVANTAGE', 19, 'Provincial priority for internationalisation and partnerships'),
 (@u, 'ADVANTAGE', 20, 'Lower living costs than tier-1 mainland cities');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'QS World University Rankings', 1201, 2026, 'Approximate band (1201-1400) / may be unranked -- verify'),
 (@u, 'THE World University Rankings', 1201, 2026, 'Approximate band (1201-1500) -- verify'),
 (@u, 'ARWU / Shanghai Ranking', 801, 2025, 'Approximate band (801-900) -- verify'),
 (@u, 'US News Best Global Universities', 950, 2025, 'Approximate -- verify'),
 (@u, 'CUAA China (Alumni Association)', 100, 2024, 'Approximate -- verify');

-- ============================================================================
-- 10. Hebei Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hebei Normal University', '河北师范大学',
   'hebei-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Shijiazhuang', 'Hebei',
   1902, 35000, NULL, NULL, 'https://www.hebtu.edu.cn/', 'Provincial key',
   'Hebei Normal University is a comprehensive provincial key university in Shijiazhuang, the capital of Hebei, and one of the oldest teacher-training institutions in China. It spans literature, history, philosophy, education, science, engineering, economics, law, management and the arts, and is the province''s principal producer of secondary-school teachers and education researchers. Mathematics, biology, chemistry, Chinese language and literature, geography and physical education are among its strongest fields, several ranked first within Hebei. The university has doctoral programmes across multiple disciplines and hosts provincial key laboratories and research bases.',
   'The university''s origins go back to two teacher-training schools founded in Beijing in 1902 and in Tianjin in 1906. Through the twentieth century these evolved and relocated, and in 1996 several Hebei teacher colleges merged in Shijiazhuang to form the present Hebei Normal University. It has since built a large new main campus and gained doctoral-degree authority in a growing number of subjects.',
   'The modern main campus in Shijiazhuang''s Yuhua district is spacious and purpose-built, with additional older sites in the city. Facilities include provincial key laboratories in the life and physical sciences, observatories and museums, sports complexes, and extensive on-campus student housing. Shijiazhuang is on the Beijing-Guangzhou high-speed rail line, about an hour from Beijing.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Verify enrolment, faculty count, doctoral-programme list and international-student intake. Founding-year is the earliest predecessor (1902); the merged university dates from 1996 -- decide which to show publicly. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Hebei Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Comprehensive provincial key university in Shijiazhuang, Hebei'),
 (@u, 'HIGHLIGHT', 2,  'One of the oldest teacher-training institutions in China (predecessors from 1902)'),
 (@u, 'HIGHLIGHT', 3,  'Principal producer of secondary-school teachers for Hebei Province'),
 (@u, 'HIGHLIGHT', 4,  '21 colleges plus an affiliated independent college'),
 (@u, 'HIGHLIGHT', 5,  '~35,000 students; 80+ undergraduate, 90+ master and 24 doctoral programmes'),
 (@u, 'HIGHLIGHT', 6,  'Mathematics and biology ranked first within Hebei Province'),
 (@u, 'HIGHLIGHT', 7,  'Provincial key laboratories in the life and physical sciences'),
 (@u, 'HIGHLIGHT', 8,  'Large purpose-built main campus in Yuhua district'),
 (@u, 'HIGHLIGHT', 9,  'Doctoral-degree authority across multiple disciplines'),
 (@u, 'HIGHLIGHT', 10, 'Formed in 1996 by merging several Hebei teacher colleges'),
 (@u, 'ADVANTAGE', 11, 'About one hour from Beijing by high-speed rail'),
 (@u, 'ADVANTAGE', 12, 'Lower tuition and living costs than Beijing / Tianjin universities'),
 (@u, 'ADVANTAGE', 13, 'Strong option for education, Chinese-language and teacher-training applicants'),
 (@u, 'ADVANTAGE', 14, 'Partnerships with 30+ universities worldwide'),
 (@u, 'ADVANTAGE', 15, 'Enrols students from Hong Kong, Macau, Taiwan and abroad'),
 (@u, 'ADVANTAGE', 16, 'Chinese-language and HSK preparation provision'),
 (@u, 'ADVANTAGE', 17, 'Comprehensive sports and cultural facilities'),
 (@u, 'ADVANTAGE', 18, 'On-campus accommodation for international students'),
 (@u, 'ADVANTAGE', 19, 'Provincial backing for internationalisation'),
 (@u, 'ADVANTAGE', 20, 'Established pathway for Chinese-Government-Scholarship students (verify)');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 120, 2024, 'Approximate -- verify'),
 (@u, 'ARWU / Best Chinese Universities', 140, 2024, 'Approximate -- verify'),
 (@u, 'Soft China Normal-University ranking', 15, 2024, 'Approximate among normal universities -- verify');

-- ============================================================================
-- 11. Hubei Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Hubei Normal University', '湖北师范大学',
   'hubei-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Huangshi', 'Hubei',
   1973, NULL, NULL, NULL, 'https://www.hbnu.edu.cn/', 'Provincial undergrad',
   'Hubei Normal University is a provincial public teaching university in Huangshi, a city on the Yangtze east of Wuhan. It is a multi-disciplinary institution built on teacher education, covering literature, science, engineering, education, economics, management, law and the arts, and trains teachers and applied professionals mainly for eastern Hubei. It has master-level programmes in several fields and provincial key disciplines and laboratories, with recognised work in physics / materials, chemistry, Chinese language education and analytical instrumentation.',
   'The university began in 1973 as the Huangshi Branch of Huazhong (Central China) Normal College, formed from an earlier branch at Daye. It became an independent provincial undergraduate college -- Huangshi Normal College -- in 1978, was renamed Hubei Normal College in 1985, and was upgraded to Hubei Normal University in 2016 after meeting the criteria for university status.',
   'The campus is on Cihu Road in Huangshi, beside Cihu Lake, a compact site with teaching buildings, provincial key laboratories, a library and sports facilities, plus on-campus student dormitories. Huangshi is about an hour from Wuhan by road or rail, giving access to the larger city''s amenities and transport hub.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10 -- limited English data. Verify enrolment, faculty count, master-programme list and international-student provision. Founding-year 1973 is the Huazhong Normal branch; university status came in 2016. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Hubei Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public teaching university in Huangshi, Hubei'),
 (@u, 'HIGHLIGHT', 2,  'Multi-disciplinary institution built on teacher education'),
 (@u, 'HIGHLIGHT', 3,  'Originated in 1973 as a branch of Huazhong (Central China) Normal College'),
 (@u, 'HIGHLIGHT', 4,  'Upgraded to full university status in 2016'),
 (@u, 'HIGHLIGHT', 5,  'Master-level programmes in several disciplines'),
 (@u, 'HIGHLIGHT', 6,  'Provincial key disciplines and laboratories in physics / materials and chemistry'),
 (@u, 'HIGHLIGHT', 7,  'Strengths in Chinese-language education and analytical instrumentation'),
 (@u, 'HIGHLIGHT', 8,  'Lakeside campus on Cihu Road beside Cihu Lake'),
 (@u, 'HIGHLIGHT', 9,  'Trains teachers and applied professionals for eastern Hubei'),
 (@u, 'HIGHLIGHT', 10, 'About one hour from Wuhan by road or rail'),
 (@u, 'ADVANTAGE', 11, 'Access to Wuhan''s transport hub and amenities'),
 (@u, 'ADVANTAGE', 12, 'Low tuition and living costs'),
 (@u, 'ADVANTAGE', 13, 'Suitable for education and Chinese-language applicants'),
 (@u, 'ADVANTAGE', 14, 'On-campus dormitories and student facilities'),
 (@u, 'ADVANTAGE', 15, 'Chinese-language study option alongside a degree'),
 (@u, 'ADVANTAGE', 16, 'Regional employer links across eastern Hubei'),
 (@u, 'ADVANTAGE', 17, 'Provincial-government support for continued development'),
 (@u, 'ADVANTAGE', 18, 'Compact, walkable campus'),
 (@u, 'ADVANTAGE', 19, 'Growing international-exchange activity (verify current partners)'),
 (@u, 'ADVANTAGE', 20, 'Open to partner-managed international recruitment');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 280, 2024, 'Approximate band -- verify'),
 (@u, 'Wu Shulian / China Academic Degrees', 260, 2024, 'Approximate -- verify');

-- ============================================================================
-- 12. Jilin Jianzhu University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Jilin Jianzhu University', '吉林建筑大学',
   'jilin-jianzhu-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Changchun', 'Jilin',
   1956, 16000, NULL, NULL, 'https://www.jlju.edu.cn/', 'Provincial key',
   'Jilin Jianzhu University is a provincial public university in the Jingyue high-tech development zone of Changchun, Jilin, and one of China''s earliest institutions dedicated to architecture and the built environment. Its core disciplines are architecture, urban and rural planning, civil engineering, municipal and environmental engineering, materials science and engineering, and management science and engineering, supported by programmes in art and design, surveying, economics and law. The university has doctoral-conferring status and is recognised for research on green and cold-climate architecture, protection of the Songhua River basin, disaster prevention in buildings, and conservation of historic structures.',
   'The university was founded in 1956 as one of the first ten architecture-oriented higher-education institutions in China. It gained authority to enrol graduate students in 2003 and was approved as a doctoral degree-conferring institution in 2017, taking its present name as it expanded from an institute into a full university.',
   'The main campus lies in Changchun''s Jingyue High-Tech Industrial Development Zone, a green district of lakes and forest parkland on the city''s southeast edge. Facilities include architecture and design studios, structural and municipal-engineering laboratories, materials-testing halls, an architecture museum and on-campus dormitories. Changchun has a subway network, high-speed rail and an international airport.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Verify enrolment (~16,000 cited), faculty count, doctoral-programme list and international provision. Strong niche in built-environment fields. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Jilin Jianzhu University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'One of China''s earliest architecture-focused universities (founded 1956)'),
 (@u, 'HIGHLIGHT', 2,  'Provincial public university in Changchun''s Jingyue high-tech zone'),
 (@u, 'HIGHLIGHT', 3,  'Doctoral degree-conferring institution since 2017'),
 (@u, 'HIGHLIGHT', 4,  'Core fields: architecture, urban planning, civil and municipal engineering'),
 (@u, 'HIGHLIGHT', 5,  'Materials science and engineering and management science and engineering strengths'),
 (@u, 'HIGHLIGHT', 6,  '19 colleges and departments; ~16,000 full-time students'),
 (@u, 'HIGHLIGHT', 7,  'Research leader in green and cold-climate architecture'),
 (@u, 'HIGHLIGHT', 8,  'Work on Songhua River basin protection and building disaster prevention'),
 (@u, 'HIGHLIGHT', 9,  'Historic-building conservation and reuse research'),
 (@u, 'HIGHLIGHT', 10, 'Architecture and design studios, structural and materials-testing laboratories'),
 (@u, 'ADVANTAGE', 11, 'Green campus setting among lakes and forest parkland'),
 (@u, 'ADVANTAGE', 12, 'Clear career pathways into design institutes and construction firms'),
 (@u, 'ADVANTAGE', 13, 'Changchun subway, high-speed rail and international airport'),
 (@u, 'ADVANTAGE', 14, 'Lower costs than eastern-seaboard universities'),
 (@u, 'ADVANTAGE', 15, 'Strong fit for built-environment and design applicants'),
 (@u, 'ADVANTAGE', 16, 'On-campus dormitories and student services'),
 (@u, 'ADVANTAGE', 17, 'Chinese-language study option alongside a degree'),
 (@u, 'ADVANTAGE', 18, 'International exchange links (verify current agreements)'),
 (@u, 'ADVANTAGE', 19, 'Provincial-government backing for development'),
 (@u, 'ADVANTAGE', 20, 'Open to partner-managed international recruitment');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 300, 2024, 'Approximate band -- verify'),
 (@u, 'Wu Shulian / China Academic Degrees', 280, 2024, 'Approximate -- verify'),
 (@u, 'ARWU subject -- Civil Engineering', 200, 2024, 'Appears in the extended list -- verify band');

-- ============================================================================
-- 13. Liaoning Petrochemical University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Liaoning Petrochemical University', '辽宁石油化工大学',
   'liaoning-petrochemical-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Fushun', 'Liaoning',
   1950, 16200, NULL, 1000, 'https://www.lnpu.edu.cn/', 'Provincial undergrad',
   'Liaoning Petrochemical University is a provincial public university in Fushun, Liaoning, and was the first petroleum-and-chemical-technology university founded in the People''s Republic of China. Its academic core is chemical engineering and technology, petroleum processing, chemical-equipment and process engineering, safety engineering, environmental engineering, automation and materials, alongside programmes in economics, management, foreign languages and the arts. The university has close ties to PetroChina, Sinopec and the wider energy sector, and its graduates are heavily represented among managers and technical staff in Chinese petroleum and chemical companies.',
   'The university was founded in 1950 in Dalian as a specialised petroleum-and-chemical technology school and relocated to Fushun -- a coal and petrochemical city -- in 1953. It developed through several names as Fushun Petroleum Institute and Liaoning Institute of Petroleum and Chemical Technology, gaining master-degree authority and, in the 2000s, adopting the name Liaoning Petrochemical University as it broadened into a multi-disciplinary institution.',
   'The campus in Fushun provides petrochemical pilot plants, process-engineering and safety-engineering laboratories, and analytical facilities, along with a library, sports grounds and on-campus dormitories. Fushun sits just east of Shenyang, the regional capital, and is connected to it by intercity rail and expressway, giving access to Shenyang''s airport and high-speed-rail hub.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Strong single-sector (petrochemical) profile and industry links. Verify enrolment, faculty count, master-programme list and international-student intake. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Liaoning Petrochemical University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'The first petroleum-and-chemical-technology university of the People''s Republic (1950)'),
 (@u, 'HIGHLIGHT', 2,  'Provincial public university in Fushun, Liaoning'),
 (@u, 'HIGHLIGHT', 3,  'Academic core in chemical engineering and petroleum processing'),
 (@u, 'HIGHLIGHT', 4,  'Process-equipment, safety and environmental engineering strengths'),
 (@u, 'HIGHLIGHT', 5,  '18 colleges and around 58 undergraduate majors'),
 (@u, 'HIGHLIGHT', 6,  '~16,000 students; master-degree authority in several fields'),
 (@u, 'HIGHLIGHT', 7,  'Close ties to PetroChina, Sinopec and the energy sector'),
 (@u, 'HIGHLIGHT', 8,  'Petrochemical pilot plants and process-engineering laboratories on campus'),
 (@u, 'HIGHLIGHT', 9,  '60,000+ alumni, many in petroleum and chemical industry leadership'),
 (@u, 'HIGHLIGHT', 10, 'Adjacent to Shenyang, the regional capital'),
 (@u, 'ADVANTAGE', 11, 'Direct pathways into oil, gas, refining and petrochemical employers'),
 (@u, 'ADVANTAGE', 12, 'Intercity rail and expressway to Shenyang''s airport and HSR hub'),
 (@u, 'ADVANTAGE', 13, 'Low tuition and living costs'),
 (@u, 'ADVANTAGE', 14, 'Strong fit for chemical / process / safety engineering applicants'),
 (@u, 'ADVANTAGE', 15, 'On-campus dormitories and student facilities'),
 (@u, 'ADVANTAGE', 16, 'Chinese-language study option alongside a degree'),
 (@u, 'ADVANTAGE', 17, 'International students already enrolled (verify numbers)'),
 (@u, 'ADVANTAGE', 18, 'Industry-sponsored laboratories and internships'),
 (@u, 'ADVANTAGE', 19, 'Provincial-government backing for development'),
 (@u, 'ADVANTAGE', 20, 'Open to partner-managed international recruitment');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 300, 2024, 'Approximate band -- verify'),
 (@u, 'Wu Shulian / China Academic Degrees', 280, 2024, 'Approximate -- verify');

-- ============================================================================
-- 14. Linyi University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Linyi University', '临沂大学',
   'linyi-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Linyi', 'Shandong',
   1941, 34500, NULL, 2900, 'https://www.lyu.edu.cn/', 'Provincial undergrad',
   'Linyi University is a large comprehensive provincial public university in Linyi, southern Shandong, with roots in a revolutionary-era teachers'' school in the Yimeng mountains. It offers 62 undergraduate degrees across nine disciplines -- economics, law, education, literature, history, science, engineering, agriculture and management -- and combines teacher education with applied engineering, agriculture, business and the geosciences. It is noted for a dinosaur-and-geoheritage research programme and museum, and for a "Yimeng spirit" civic-education identity. The university has more than 2,900 staff and around 1,150 doctorate-holding teachers, and partnerships with over 100 universities in 26 countries.',
   'The university traces its origin to 1941 and a school attached to the Anti-Japanese military and political cadre training in the Shandong base area. It developed into Linyi Normal School and then Linyi Normal University, and in 2010 the Ministry of Education approved its change of name to Linyi University, reflecting its move to a broad comprehensive profile on a large new campus.',
   'The main campus in Linyi is one of the largest single university campuses in China by area, with wide boulevards, lakes, a natural-history / dinosaur museum, extensive laboratories and sports facilities, and on-campus student accommodation. Linyi is a commercial and logistics centre in southern Shandong, served by high-speed rail and an airport, within reach of Qingdao and Jinan.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Large intake, active internationally -- likely receptive to partner recruitment. Verify enrolment, faculty count and current scholarship terms. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Linyi University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Large comprehensive provincial public university in Linyi, Shandong'),
 (@u, 'HIGHLIGHT', 2,  'Roots in a 1941 revolutionary-era teachers'' school in the Yimeng mountains'),
 (@u, 'HIGHLIGHT', 3,  '62 undergraduate degrees across nine disciplines'),
 (@u, 'HIGHLIGHT', 4,  '~34,500 full-time students; ~50,000 including continuing education'),
 (@u, 'HIGHLIGHT', 5,  '2,900+ staff, including ~1,150 teachers with doctorates'),
 (@u, 'HIGHLIGHT', 6,  'Renowned dinosaur and geoheritage research programme and museum'),
 (@u, 'HIGHLIGHT', 7,  'Combines teacher education with applied engineering, agriculture and business'),
 (@u, 'HIGHLIGHT', 8,  'One of the largest single university campuses in China by area'),
 (@u, 'HIGHLIGHT', 9,  'National and provincial talent-scheme scholars on the faculty'),
 (@u, 'HIGHLIGHT', 10, 'Renamed Linyi University in 2010 on becoming comprehensive'),
 (@u, 'ADVANTAGE', 11, 'Partnerships with 100+ universities across 26 countries'),
 (@u, 'ADVANTAGE', 12, 'Operates a Confucius Institute in Guinea'),
 (@u, 'ADVANTAGE', 13, 'High-speed rail and airport in Linyi; near Qingdao and Jinan'),
 (@u, 'ADVANTAGE', 14, 'Low tuition and living costs'),
 (@u, 'ADVANTAGE', 15, 'Spacious modern campus with strong sports and lab facilities'),
 (@u, 'ADVANTAGE', 16, 'Chinese-language and scholarship pathways for international students (verify)'),
 (@u, 'ADVANTAGE', 17, 'Broad programme choice suits undecided applicants'),
 (@u, 'ADVANTAGE', 18, 'On-campus accommodation for international students'),
 (@u, 'ADVANTAGE', 19, 'Regional employer network across Shandong'),
 (@u, 'ADVANTAGE', 20, 'Institutional appetite for international student growth');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 320, 2024, 'Approximate band -- verify'),
 (@u, 'Wu Shulian / China Academic Degrees', 300, 2024, 'Approximate -- verify');

-- ============================================================================
-- 15. Nanjing Forestry University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Nanjing Forestry University', '南京林业大学',
   'nanjing-forestry-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Nanjing', 'Jiangsu',
   1952, 34500, NULL, 1200, 'https://www.njfu.edu.cn/', 'Double First-Class',
   'Nanjing Forestry University is a provincial public university in Nanjing, Jiangsu, jointly built by the National Forestry and Grassland Administration and the province, and part of the Double First-Class initiative with forestry engineering as a world-class discipline. It is one of China''s two leading forestry universities, strong in forestry, forest products and wood science, pulp and paper, landscape architecture, ecology, biology, chemical engineering of forest products, and civil and transport engineering. Its forestry-engineering and related subjects rank among the best in China and appear near the top of global forestry subject rankings.',
   'The university originates from the forestry departments of Jinling University and National Central University. In the 1952 national reorganisation of higher education these, together with forestry programmes from Wuhan and Nanchang, were merged to create Nanjing Forestry College. It was renamed the Nanjing Technological College of Forest Products in 1972 and adopted its present name in 1985, later joining the Double First-Class list.',
   'The main Xuanwu campus lies at the foot of Zijin (Purple) Mountain beside Xuanwu Lake in northeast Nanjing, an arboretum-like site with mature plantings, a bamboo garden and a wood-specimen museum, complemented by a suburban Huai''an campus. Facilities include forestry and wood-science laboratories, wind-tunnel and materials halls, and on-campus student housing. Nanjing is a major high-speed-rail hub with an international airport.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Double First-Class in forestry engineering -- credible research partner. Verify enrolment, faculty count and current Double First-Class discipline list. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Nanjing Forestry University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'One of China''s two leading forestry universities'),
 (@u, 'HIGHLIGHT', 2,  'Double First-Class institution -- forestry engineering a world-class discipline'),
 (@u, 'HIGHLIGHT', 3,  'Jointly built by the National Forestry and Grassland Administration and Jiangsu'),
 (@u, 'HIGHLIGHT', 4,  'Strengths in wood science, pulp and paper, and forest-products chemical engineering'),
 (@u, 'HIGHLIGHT', 5,  'Leading landscape-architecture and ecology programmes'),
 (@u, 'HIGHLIGHT', 6,  '~34,500 students across two campuses (Xuanwu, Huai''an)'),
 (@u, 'HIGHLIGHT', 7,  'Forestry subject ranked near the top globally in ARWU subject rankings'),
 (@u, 'HIGHLIGHT', 8,  'Campus at the foot of Purple Mountain beside Xuanwu Lake'),
 (@u, 'HIGHLIGHT', 9,  'Arboretum, bamboo garden and wood-specimen museum on site'),
 (@u, 'HIGHLIGHT', 10, 'Roots in the forestry departments of Jinling and National Central universities'),
 (@u, 'ADVANTAGE', 11, 'Nanjing is a major HSR hub with an international airport'),
 (@u, 'ADVANTAGE', 12, 'Strong research funding and graduate-study opportunities'),
 (@u, 'ADVANTAGE', 13, 'Good fit for forestry, materials, environment and design applicants'),
 (@u, 'ADVANTAGE', 14, 'English-taught graduate programmes in selected fields (verify)'),
 (@u, 'ADVANTAGE', 15, 'Chinese Government and provincial scholarships (verify current terms)'),
 (@u, 'ADVANTAGE', 16, 'International partnership and joint-research network'),
 (@u, 'ADVANTAGE', 17, 'On-campus accommodation for international students'),
 (@u, 'ADVANTAGE', 18, 'Attractive green campus environment'),
 (@u, 'ADVANTAGE', 19, 'Jiangsu''s strong graduate labour market'),
 (@u, 'ADVANTAGE', 20, 'Recognised specialist brand internationally');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'THE World University Rankings', 801, 2026, 'Approximate band (801-1000) -- verify'),
 (@u, 'ARWU / Shanghai Ranking', 401, 2025, 'Approximate band (401-500) -- verify'),
 (@u, 'US News Best Global Universities', 650, 2025, 'Approximate -- verify'),
 (@u, 'CUAA China (Alumni Association)', 95, 2024, 'Approximate -- verify'),
 (@u, 'ARWU subject -- Forestry', 3, 2024, 'Top-3 globally in recent editions -- verify exact position');

-- ============================================================================
-- 16. Nanjing University of Posts and Telecommunications
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Nanjing University of Posts and Telecommunications', '南京邮电大学',
   'nanjing-university-of-posts-and-telecommunications', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Nanjing', 'Jiangsu',
   1942, 30000, NULL, 2280, 'https://www.njupt.edu.cn/', 'Double First-Class',
   'Nanjing University of Posts and Telecommunications (NJUPT) is a provincial public university in Nanjing, Jiangsu, on the Double First-Class list with telecommunications science and technology as its designated world-class discipline. It is one of China''s foremost universities for information and communications technology, strong in telecommunications engineering, electronic science, signal processing, integrated circuits, materials for optoelectronics and organic electronics, computer science, cybersecurity, the Internet of Things, and management of information industries. It has deep links to China''s telecom operators and equipment makers and a long record of supplying engineers to the sector.',
   'NJUPT traces its origins to a 1942 wartime communications training school in the Central China anti-Japanese base area. It was formally established as an institute of posts and telecommunications in Nanjing in 1958 under the then Ministry of Posts and Telecommunications, becoming a university and later joining the Double First-Class list with its telecommunications discipline.',
   'NJUPT has several campuses in Nanjing -- Xianlin, Sanpailou, Suojincun and Jiangning -- with the large Xianlin campus in the eastern university district hosting most undergraduate teaching. Facilities include national and provincial key laboratories in communications, optoelectronics and organic electronics, IC design suites and IoT testbeds, plus libraries, sports centres and on-campus dormitories. Nanjing is a major high-speed-rail hub with an international airport.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Double First-Class in telecommunications -- strong ICT partner. Verify enrolment, faculty count, campus allocation and current international-programme list. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Nanjing University of Posts and Telecommunications' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Leading Chinese university for information and communications technology'),
 (@u, 'HIGHLIGHT', 2,  'Double First-Class -- telecommunications science and technology'),
 (@u, 'HIGHLIGHT', 3,  'Strengths in telecom engineering, electronics, ICs and signal processing'),
 (@u, 'HIGHLIGHT', 4,  'Recognised research in organic and optoelectronic materials'),
 (@u, 'HIGHLIGHT', 5,  'Internet of Things and cybersecurity programmes'),
 (@u, 'HIGHLIGHT', 6,  '16 schools; ~30,000 students'),
 (@u, 'HIGHLIGHT', 7,  'Origins in a 1942 wartime communications training school'),
 (@u, 'HIGHLIGHT', 8,  'National and provincial key laboratories in communications and optoelectronics'),
 (@u, 'HIGHLIGHT', 9,  'Deep links to China''s telecom operators and equipment makers'),
 (@u, 'HIGHLIGHT', 10, 'Best national ranking around 76th (Shanghai Ranking, 2019)'),
 (@u, 'ADVANTAGE', 11, 'Multiple Nanjing campuses; main teaching on the large Xianlin campus'),
 (@u, 'ADVANTAGE', 12, 'Excellent graduate employment in the ICT sector'),
 (@u, 'ADVANTAGE', 13, 'Nanjing HSR hub and international airport'),
 (@u, 'ADVANTAGE', 14, 'English-taught programmes in ICT fields (verify current list)'),
 (@u, 'ADVANTAGE', 15, 'Chinese Government and provincial scholarships (verify terms)'),
 (@u, 'ADVANTAGE', 16, 'International joint programmes and exchange links'),
 (@u, 'ADVANTAGE', 17, 'IC design suites and IoT testbeds for hands-on work'),
 (@u, 'ADVANTAGE', 18, 'On-campus accommodation for international students'),
 (@u, 'ADVANTAGE', 19, 'Jiangsu''s strong technology labour market'),
 (@u, 'ADVANTAGE', 20, 'Clear specialist identity valued by employers');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'THE World University Rankings', 801, 2026, 'Approximate band (801-1000) -- verify'),
 (@u, 'ARWU / Shanghai Ranking', 501, 2025, 'Approximate band (501-600) -- verify'),
 (@u, 'US News Best Global Universities', 700, 2025, 'Approximate -- verify'),
 (@u, 'Best Chinese Universities', 80, 2024, 'Approximate (best cited ~76 in 2019) -- verify'),
 (@u, 'CUAA China (Alumni Association)', 100, 2024, 'Approximate -- verify');

-- ============================================================================
-- 17. Nantong University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Nantong University', '南通大学',
   'nantong-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Nantong', 'Jiangsu',
   1912, 32000, 750, 3152, 'https://www.ntu.edu.cn/', 'Provincial key',
   'Nantong University is a comprehensive provincial public university in Nantong, Jiangsu, co-funded by the province and the Ministry of Transport. It has a strong medical tradition -- its clinical medicine and neuroscience research (notably nerve-injury repair) are nationally recognised and its medicine programmes are accredited for international students -- alongside engineering, information science, textiles, marine and transport engineering, education, economics and the humanities. It offers 106 undergraduate programmes and hosts doctoral and master programmes, provincial key laboratories and a well-known affiliated hospital network.',
   'The university descends from three institutions founded by the late-Qing industrialist and educator Zhang Jian: a normal school (1902-05), a medical school (1912) and a textile school (1912) in Nantong. These evolved into Nantong Medical College, Nantong Institute of Technology and Nantong Normal College, which merged in 2004 to form Nantong University.',
   'Nantong University has four campuses -- the new (main) campus plus the Qixiu, Zhongxiu and Qidong sites -- in and around Nantong, a Yangtze-estuary city directly across the river from Shanghai and now linked to it by rail bridge. Facilities include teaching hospitals, medical and life-science laboratories, engineering and textile workshops, libraries and on-campus dormitories for domestic and international students.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Established international medical intake (~750 international students cited). Verify enrolment, faculty count, campus allocation and MBBS/medicine accreditation status before publishing. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Nantong University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Comprehensive provincial public university co-funded by Jiangsu and the Ministry of Transport'),
 (@u, 'HIGHLIGHT', 2,  'Founded on schools established by reformer-industrialist Zhang Jian (from 1912)'),
 (@u, 'HIGHLIGHT', 3,  'Nationally recognised clinical medicine and neuroscience research'),
 (@u, 'HIGHLIGHT', 4,  'Internationally accredited medicine programmes; ~750 international students'),
 (@u, 'HIGHLIGHT', 5,  '13 schools; 106 undergraduate programmes across nine disciplines'),
 (@u, 'HIGHLIGHT', 6,  'Doctoral and master programmes; provincial key laboratories'),
 (@u, 'HIGHLIGHT', 7,  'Strong engineering, textile, marine and transport-engineering fields'),
 (@u, 'HIGHLIGHT', 8,  'Network of affiliated teaching hospitals'),
 (@u, 'HIGHLIGHT', 9,  'Four campuses in and around Nantong'),
 (@u, 'HIGHLIGHT', 10, 'Ranked around 100th among Chinese universities (CUAA, approximate)'),
 (@u, 'ADVANTAGE', 11, 'Directly across the Yangtze from Shanghai, now rail-linked'),
 (@u, 'ADVANTAGE', 12, 'Established English-taught MBBS / medicine track for international students (verify)'),
 (@u, 'ADVANTAGE', 13, 'Lower cost base than Shanghai with Shanghai access'),
 (@u, 'ADVANTAGE', 14, 'Scholarships for international students (verify current terms)'),
 (@u, 'ADVANTAGE', 15, 'On-campus accommodation for international students'),
 (@u, 'ADVANTAGE', 16, 'Clinical placement capacity through affiliated hospitals'),
 (@u, 'ADVANTAGE', 17, 'Jiangsu''s strong graduate labour market'),
 (@u, 'ADVANTAGE', 18, 'Broad programme choice for undecided applicants'),
 (@u, 'ADVANTAGE', 19, 'Experienced international-student administration'),
 (@u, 'ADVANTAGE', 20, 'Receptive to partner-managed recruitment for medicine and engineering');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 950, 2025, 'Approximate -- verify'),
 (@u, 'CUAA China (Alumni Association)', 100, 2024, 'Approximate -- verify'),
 (@u, 'ARWU / Best Chinese Universities', 120, 2024, 'Approximate -- verify');

-- ============================================================================
-- 18. Northwestern Polytechnical University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Northwestern Polytechnical University', '西北工业大学',
   'northwestern-polytechnical-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Xi''an', 'Shaanxi',
   1938, 28000, NULL, 3600, 'https://www.nwpu.edu.cn/', 'Double First-Class',
   'Northwestern Polytechnical University (NWPU) is a top-tier national research university in Xi''an, Shaanxi, supervised by the Ministry of Industry and Information Technology and part of Project 985, Project 211 and the Double First-Class initiative. It is the only Chinese university that simultaneously develops aeronautics, astronautics and marine (underwater) engineering, and is a leader in aerospace, materials science, mechanical and control engineering, computer science, and unmanned systems. It runs national key laboratories and has produced a large share of China''s aerospace chief designers.',
   'NWPU grew out of engineering institutions displaced westward during the Japanese invasion from 1938. Its present form dates to 1957, when the Northwestern Institute of Technology merged with the aeronautics department of the Xi''an-based military aviation institute; a marine-engineering institute from Harbin was incorporated in 1970. It joined Project 211 and Project 985 in the 1990s and the Double First-Class list in 2017.',
   'NWPU spans about 4.6 square kilometres over three sites: the older Youyi campus in Beilin district of Xi''an, the large Chang''an campus to the south, and a Taicang campus near Suzhou. Facilities include wind tunnels, flight-simulation and materials laboratories, an aeronautics museum with historic aircraft, high-performance computing, libraries and on-campus student housing. Xi''an is a major high-speed-rail hub with an international airport.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Elite 985 university with aerospace / defence heritage -- some programmes may be closed or restricted for certain international applicants; confirm eligibility by field and nationality. Verify enrolment / faculty and Taicang-campus provision. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Northwestern Polytechnical University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Project 985, Project 211 and Double First-Class national research university'),
 (@u, 'HIGHLIGHT', 2,  'Only Chinese university spanning aeronautics, astronautics and marine engineering together'),
 (@u, 'HIGHLIGHT', 3,  'Leader in aerospace, materials science and unmanned systems'),
 (@u, 'HIGHLIGHT', 4,  '15 schools; 58 undergraduate, 117 master and 67 doctoral programmes'),
 (@u, 'HIGHLIGHT', 5,  '~28,000 students; ~3,600 faculty'),
 (@u, 'HIGHLIGHT', 6,  'Three campuses: Youyi and Chang''an in Xi''an, plus Taicang near Suzhou'),
 (@u, 'HIGHLIGHT', 7,  'Wind tunnels, flight-simulation and materials laboratories'),
 (@u, 'HIGHLIGHT', 8,  'Aeronautics museum with historic aircraft on campus'),
 (@u, 'HIGHLIGHT', 9,  'QS World ~499; ARWU world 101-150; US News global ~182 (verify current)'),
 (@u, 'HIGHLIGHT', 10, 'Major supplier of China''s aerospace chief designers'),
 (@u, 'ADVANTAGE', 11, 'National key laboratories and heavy research funding'),
 (@u, 'ADVANTAGE', 12, 'Xi''an is a major HSR hub with an international airport'),
 (@u, 'ADVANTAGE', 13, 'Strong graduate outcomes in aerospace, defence, materials and IT'),
 (@u, 'ADVANTAGE', 14, 'English-taught degree programmes for international students (verify list)'),
 (@u, 'ADVANTAGE', 15, 'Chinese Government Scholarship and NWPU scholarships (verify terms)'),
 (@u, 'ADVANTAGE', 16, 'Wide international partnership network'),
 (@u, 'ADVANTAGE', 17, 'Lower living costs than eastern tier-1 cities'),
 (@u, 'ADVANTAGE', 18, 'Taicang campus offers a Yangtze-Delta option under the same brand'),
 (@u, 'ADVANTAGE', 19, 'Strong student research and competition record'),
 (@u, 'ADVANTAGE', 20, 'High prestige and recognisability with employers');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'QS World University Rankings', 499, 2026, 'Per ~2026 edition -- re-check yearly'),
 (@u, 'THE World University Rankings', 251, 2026, 'Approximate band (251-300) -- verify'),
 (@u, 'ARWU / Shanghai Ranking', 101, 2025, 'Approximate band (101-150) -- verify'),
 (@u, 'US News Best Global Universities', 182, 2025, 'Approximate -- verify'),
 (@u, 'CUAA China (Alumni Association)', 27, 2024, 'Approximate -- verify');

-- ============================================================================
-- 19. Qujing Normal University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Qujing Normal University', '曲靖师范学院',
   'qujing-normal-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Qujing', 'Yunnan',
   1907, 15000, NULL, NULL, 'https://www.qjnu.edu.cn/', 'Provincial undergrad',
   'Qujing Normal University is a provincial public undergraduate university in Qujing, the second-largest city of Yunnan Province in southwest China. It is primarily a teacher-training institution, offering programmes across education, literature, science, engineering, economics, management, law and the arts, and supplying teachers and public-service professionals to eastern Yunnan and the wider region. It has around 15,000 full-time students drawn from 26 provinces and a set of teaching and research units covering the main school subjects and selected applied fields.',
   'Its history is traced to a teacher-training school established in 1907, which became the Yunnan Provincial Third Normal School in 1912. In March 2000 three local institutions -- Qujing Senior Teachers College, Qujing Education College (founded 1984) and Qujing Normal School -- merged and were approved by the Ministry of Education as Qujing Normal University, a full undergraduate institution.',
   'The campus is in Qujing, a city on the Yunnan-Guizhou plateau at high elevation with a mild year-round climate, on the rail and expressway corridor between Kunming and Guiyang. It provides standard teaching buildings, laboratories, library, sports facilities and on-campus dormitories. Kunming, the provincial capital with an international airport, is about an hour away by high-speed rail.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10 -- minimal English data. Verify enrolment, faculty count, programme list and whether it enrols international students at all. Founding-year 1907 is the earliest predecessor; the merged university dates from 2000. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Qujing Normal University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public teacher-training university in Qujing, Yunnan'),
 (@u, 'HIGHLIGHT', 2,  'Second-largest city in Yunnan, in southwest China'),
 (@u, 'HIGHLIGHT', 3,  'History traced to a 1907 teacher-training school'),
 (@u, 'HIGHLIGHT', 4,  'Formed in 2000 by merging three local teacher-education institutions'),
 (@u, 'HIGHLIGHT', 5,  '~15,000 full-time students from 26 provinces'),
 (@u, 'HIGHLIGHT', 6,  'Around 16 teaching and research units'),
 (@u, 'HIGHLIGHT', 7,  'Programmes across education, literature, science, engineering and management'),
 (@u, 'HIGHLIGHT', 8,  'Key regional supplier of schoolteachers for eastern Yunnan'),
 (@u, 'HIGHLIGHT', 9,  'Mild high-plateau climate year round'),
 (@u, 'HIGHLIGHT', 10, 'On the Kunming-Guiyang rail and expressway corridor'),
 (@u, 'ADVANTAGE', 11, 'About one hour from Kunming by high-speed rail'),
 (@u, 'ADVANTAGE', 12, 'Very low tuition and living costs'),
 (@u, 'ADVANTAGE', 13, 'Suitable for education and Chinese-language applicants'),
 (@u, 'ADVANTAGE', 14, 'On-campus dormitories and student facilities'),
 (@u, 'ADVANTAGE', 15, 'Chinese-language study option alongside a degree'),
 (@u, 'ADVANTAGE', 16, 'Gateway location toward Southeast Asia'),
 (@u, 'ADVANTAGE', 17, 'Provincial-government backing for development'),
 (@u, 'ADVANTAGE', 18, 'Quiet mid-size-city study environment'),
 (@u, 'ADVANTAGE', 19, 'Emerging international-exchange activity (verify)'),
 (@u, 'ADVANTAGE', 20, 'Open to partner-managed international recruitment');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 500, 2024, 'Approximate band (400-600) -- verify'),
 (@u, 'Wu Shulian / China Academic Degrees', 480, 2024, 'Approximate -- verify');

-- ============================================================================
-- 20. Shenyang Medical College
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Shenyang Medical College', '沈阳医学院',
   'shenyang-medical-college', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Shenyang', 'Liaoning',
   1949, NULL, NULL, NULL, 'https://www.symc.edu.cn/', 'Provincial undergrad',
   'Shenyang Medical College is a provincial public medical college in Shenyang, the capital of Liaoning Province in northeast China. It offers undergraduate programmes in clinical medicine, preventive medicine, stomatology, nursing, pharmacy, medical imaging, rehabilitation and related health sciences, supported by affiliated and teaching hospitals in the city. It has admitted international students since 2004, primarily in clinical medicine (MBBS), and focuses on training front-line doctors, public-health workers and nurses for the region.',
   'The college was founded in 1949 as the Shenyang Municipal Advanced Practice Nurse School. It was reorganised under provincial administration and renamed several times, becoming Shenyang Advanced Medical School and then, in 1987, Shenyang Medical College with undergraduate degree authority. It began enrolling international students in 2004.',
   'The campus is in Shenyang, a major industrial and transport centre with a metro network, high-speed rail and an international airport. It provides anatomy and pathology laboratories, simulation and skills-training facilities, a medical library and on-campus dormitories, with clinical training in affiliated hospitals across the city.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10 -- thin public data. Verify enrolment, faculty count, whether it holds full "university" or "college" status, and current MBBS / international-student accreditation and China MoE listing. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Shenyang Medical College' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public medical college in Shenyang, Liaoning'),
 (@u, 'HIGHLIGHT', 2,  'Founded 1949; undergraduate medical college since 1987'),
 (@u, 'HIGHLIGHT', 3,  'Programmes in clinical medicine, preventive medicine, stomatology and nursing'),
 (@u, 'HIGHLIGHT', 4,  'Pharmacy, medical imaging and rehabilitation programmes'),
 (@u, 'HIGHLIGHT', 5,  'Admits international students since 2004, primarily MBBS'),
 (@u, 'HIGHLIGHT', 6,  'Affiliated and teaching hospitals across Shenyang'),
 (@u, 'HIGHLIGHT', 7,  'Anatomy, pathology and clinical-skills simulation facilities'),
 (@u, 'HIGHLIGHT', 8,  'Focus on training front-line doctors, public-health workers and nurses'),
 (@u, 'HIGHLIGHT', 9,  'Located in a provincial capital with strong healthcare infrastructure'),
 (@u, 'HIGHLIGHT', 10, 'Shenyang metro, high-speed rail and international airport'),
 (@u, 'ADVANTAGE', 11, 'Lower tuition and living costs than eastern tier-1 cities'),
 (@u, 'ADVANTAGE', 12, 'Established English-taught MBBS pathway (verify accreditation)'),
 (@u, 'ADVANTAGE', 13, 'Clinical placement capacity through affiliated hospitals'),
 (@u, 'ADVANTAGE', 14, 'On-campus dormitories for international students'),
 (@u, 'ADVANTAGE', 15, 'Chinese-language and medical-terminology support (verify)'),
 (@u, 'ADVANTAGE', 16, 'Experienced in hosting international medical students'),
 (@u, 'ADVANTAGE', 17, 'Regional employer and hospital network'),
 (@u, 'ADVANTAGE', 18, 'Compact, focused single-discipline institution'),
 (@u, 'ADVANTAGE', 19, 'Provincial-government backing'),
 (@u, 'ADVANTAGE', 20, 'Receptive to partner-managed recruitment for medicine');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China -- medical colleges', 90, 2024, 'Approximate among medical schools -- verify'),
 (@u, 'Wu Shulian / China Academic Degrees', 400, 2024, 'Approximate overall band -- verify');

-- ============================================================================
-- 21. Suqian University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Suqian University', '宿迁学院',
   'suqian-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Suqian', 'Jiangsu',
   2002, 17000, NULL, 800, 'https://www.squ.edu.cn/', 'Provincial undergrad',
   'Suqian University is a provincial public undergraduate university in Suqian, northern Jiangsu, established in 2002 with support from eight leading universities in the province. It is an application-oriented institution with around 17,000 full-time students and nine schools, offering roughly 43-54 undergraduate majors across engineering, science, literature, arts, education, management, economics, law and agriculture. It is closely aligned with the local economy -- electronics and information, machinery, textiles, food and e-commerce (Suqian is the home city of JD.com''s founder and a major online-retail logistics base).',
   'The university was approved in June 2002 by the Jiangsu provincial government, initially run co-operatively with eight established Jiangsu universities (including Jiangsu University, Soochow University, Yangzhou University and several Nanjing institutions) providing academic support. Over time it consolidated into a standalone provincial public undergraduate university under the name Suqian University; its exact current registration and public/private status should be confirmed.',
   'The campus is in Suqian, a young prefecture-level city on the Grand Canal in northern Jiangsu, near Luoma Lake and Hongze Lake. It provides modern teaching buildings, engineering and computing laboratories, a library, sports facilities and on-campus dormitories. Suqian has expressway and rail connections to Xuzhou, Huai''an and Nanjing.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10. Sources disagree on public vs private and on major count -- CONFIRM current status, website (squ.edu.cn assumed), enrolment and whether it enrols international students. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Suqian University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Provincial public application-oriented university in Suqian, Jiangsu'),
 (@u, 'HIGHLIGHT', 2,  'Established 2002 with support from eight leading Jiangsu universities'),
 (@u, 'HIGHLIGHT', 3,  '~17,000 full-time students; nine schools'),
 (@u, 'HIGHLIGHT', 4,  '40+ undergraduate majors across engineering, science, arts and management'),
 (@u, 'HIGHLIGHT', 5,  'Curriculum aligned with local electronics, machinery, textile and food industries'),
 (@u, 'HIGHLIGHT', 6,  'Suqian is a major e-commerce and online-retail logistics base'),
 (@u, 'HIGHLIGHT', 7,  'Academic support historically from Soochow, Jiangsu, Yangzhou and Nanjing universities'),
 (@u, 'HIGHLIGHT', 8,  'Modern campus near the Grand Canal and Luoma Lake'),
 (@u, 'HIGHLIGHT', 9,  'Engineering and computing laboratories for applied training'),
 (@u, 'HIGHLIGHT', 10, 'Cooperative agreements with universities in the US, Canada, Korea, Italy and elsewhere'),
 (@u, 'ADVANTAGE', 11, 'Low tuition and living costs'),
 (@u, 'ADVANTAGE', 12, 'Expressway and rail links to Xuzhou, Huai''an and Nanjing'),
 (@u, 'ADVANTAGE', 13, 'Strong local internship and employment pipeline in e-commerce and manufacturing'),
 (@u, 'ADVANTAGE', 14, 'Newer campus and facilities'),
 (@u, 'ADVANTAGE', 15, 'Application-focused teaching suits vocational-minded applicants'),
 (@u, 'ADVANTAGE', 16, 'On-campus dormitories and student services'),
 (@u, 'ADVANTAGE', 17, 'Chinese-language study option alongside a degree'),
 (@u, 'ADVANTAGE', 18, 'Jiangsu''s strong graduate labour market'),
 (@u, 'ADVANTAGE', 19, 'Provincial-government backing for development'),
 (@u, 'ADVANTAGE', 20, 'Open to partner-managed international recruitment');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 400, 2024, 'Approximate band (350-500) -- verify'),
 (@u, 'Wu Shulian / China Academic Degrees', 420, 2024, 'Approximate -- verify');

-- ============================================================================
-- 22. Tianjin Foreign Studies University
-- ============================================================================
set @rc := @rc + 1;
insert ignore into nad_university
  (name, name_cn, slug, reference_code, partner_status, country, type, city, province,
   founded_year, total_students, international_students, faculty_count, website, ranking_tier,
   introduction, history, campus_info,
   is_recommended, is_featured, public_partner, status, publish_status, remark, create_by, create_time)
values
  ('Tianjin Foreign Studies University', '天津外国语大学',
   'tianjin-foreign-studies-university', concat('NAD-UNI-', lpad(@rc, 4, '0')),
   'PROSPECT', 'CN', 'PUBLIC', 'Tianjin', 'Tianjin',
   1964, NULL, NULL, NULL, 'https://www.tjfsu.edu.cn/', 'Provincial key',
   'Tianjin Foreign Studies University (TFSU) is a municipal public university in Tianjin specialising in foreign languages and international studies. It teaches a wide range of languages -- English, Japanese, Korean, Russian, French, German, Spanish, Arabic and others -- together with translation and interpreting, international business, law, journalism, international relations, economics and area studies. It is one of a small group of dedicated foreign-studies universities in China and a well-regarded destination for Chinese-language study and translator training, with a strong record of placing graduates in diplomacy, trade, media and education.',
   'The university originates from a 1964 foreign-languages school in Tianjin. In 1974 several language programmes were consolidated into Tianjin Foreign Languages College, which gained master-degree authority in 1981 and was upgraded to Tianjin Foreign Studies University in 2010. It occupies a historic site associated with the former Sino-French and concession-era institutions of Tianjin.',
   'The Machang campus in the Wudadao ("Five Avenues") historic district of Tianjin features preserved early-twentieth-century European-style buildings, with a newer larger campus in the Binhai / Xiqing area for most undergraduate teaching. Facilities include language laboratories, simultaneous-interpreting suites, a foreign-language library and on-campus dormitories. Tianjin is 30 minutes from Beijing by high-speed rail and has its own international airport.',
   0, 0, 0, 'ACTIVE', 'DRAFT',
   'Internal: imported 2026-09-10 -- limited English data. Verify enrolment, faculty count, campus split (Machang vs Binhai) and current international-programme and scholarship list. Strong fit for Chinese-language and translation students. Partner status PROSPECT; no MoU on file.',
   'import', now());
set @u := (select id from nad_university where name = 'Tianjin Foreign Studies University' and country = 'CN');
insert into nad_university_highlight (university_id, kind, sort_order, text) values
 (@u, 'HIGHLIGHT', 1,  'Municipal public university specialising in foreign languages and international studies'),
 (@u, 'HIGHLIGHT', 2,  'One of a small group of dedicated foreign-studies universities in China'),
 (@u, 'HIGHLIGHT', 3,  'Teaches English, Japanese, Korean, Russian, French, German, Spanish, Arabic and more'),
 (@u, 'HIGHLIGHT', 4,  'Strong translation and interpreting (including simultaneous-interpreting) training'),
 (@u, 'HIGHLIGHT', 5,  'Programmes in international business, law, journalism and international relations'),
 (@u, 'HIGHLIGHT', 6,  'Originated in 1964; university status since 2010'),
 (@u, 'HIGHLIGHT', 7,  'Historic Machang campus in Tianjin''s Five Avenues heritage district'),
 (@u, 'HIGHLIGHT', 8,  'Newer main campus in the Binhai / Xiqing area'),
 (@u, 'HIGHLIGHT', 9,  'Language laboratories and interpreting suites'),
 (@u, 'HIGHLIGHT', 10, 'Strong graduate placement in diplomacy, trade, media and education'),
 (@u, 'ADVANTAGE', 11, '30 minutes from Beijing by high-speed rail; own international airport'),
 (@u, 'ADVANTAGE', 12, 'Well suited to Chinese-language study and translator training'),
 (@u, 'ADVANTAGE', 13, 'Lower living costs than Beijing'),
 (@u, 'ADVANTAGE', 14, 'International joint programmes and exchange links (e.g. recent Aberdeen partnership)'),
 (@u, 'ADVANTAGE', 15, 'Scholarships for international / Chinese-language students (verify terms)'),
 (@u, 'ADVANTAGE', 16, 'On-campus accommodation for international students'),
 (@u, 'ADVANTAGE', 17, 'Cosmopolitan port-city environment with concession-era heritage'),
 (@u, 'ADVANTAGE', 18, 'Small-cohort language teaching'),
 (@u, 'ADVANTAGE', 19, 'Established HSK preparation and pre-university Chinese programmes (verify)'),
 (@u, 'ADVANTAGE', 20, 'Receptive to partner-managed recruitment for language programmes');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'CUAA China (Alumni Association)', 240, 2024, 'Approximate band -- verify'),
 (@u, 'Wu Shulian / China Academic Degrees', 220, 2024, 'Approximate -- verify'),
 (@u, 'CUAA -- foreign-studies unis', 6, 2024, 'Approximate among foreign-studies universities -- verify');
