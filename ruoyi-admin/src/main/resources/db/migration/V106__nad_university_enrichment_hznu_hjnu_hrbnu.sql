-- Catalog enrichment for three universities imported (as drafts) by V90: Hangzhou Normal University,
-- Hanjiang Normal University and Harbin Normal University. DML only, no schema change. Rows stay DRAFT.
--
-- Provenance (checked 2026-10-19; every image below was downloaded and looked at before being referenced):
--   Hangzhou Normal University -- www.hznu.edu.cn (院系设置 page, 校园风光 page, English site) and the official 2025
--     international student brochure (gjjyxy.hznu.edu.cn/c/2025-03-24/3063770.shtml) for housing and headline figures.
--   Hanjiang Normal University -- www.hjnu.edu.cn (学校简介, 学校标识, 机构设置 pages).
--   Harbin Normal University -- the official site could not be reached from our network, so the college list comes from
--     the Chinese Wikipedia article and the dormitory facts from CUCAS (hrbnu.cucas.cn, updated 2024-11-22). Both are
--     third-party: staff should confirm against hrbnu.edu.cn. No Harbin images are added -- none could be verified.
--
-- Images are the universities' own files, referenced by URL through the legacy *_image_url columns / gallery image_url.
-- Nothing is copied into our storage, so a university redesigning its site can break a link. Staff can replace any of
-- them with an upload from the admin screen.
--
-- Department names are the official Chinese names; the English names are plain translations (the sites publish no
-- English list), so staff may prefer to polish them.
--
-- Also corrected, because the V90 figures contradicted the official sources:
--   Hangzhou Normal: total_students 22000 -> 32491, international_students 2000 -> 450 (degree students),
--     faculty_count 1900 -> 3118 (all staff).
--   Hanjiang Normal: total_students 15300 -> 18708 (full-time, May 2026).
--
-- Manual rollback:
--   delete from nad_department where create_by = 'import-v106';
--   delete from nad_university_gallery where university_id in
--     (select id from nad_university where name = 'Hangzhou Normal University' and country = 'CN');
--   update nad_university set logo_image_url = null, cover_image_url = null, accommodation_info = null, nearby_info = null
--     where country = 'CN' and name in ('Hangzhou Normal University', 'Hanjiang Normal University', 'Harbin Normal University');
--   (the V90 figures above would need to be restored by hand: 22000 / 2000 / 1900 and 15300)

-- ============================================================================
-- Hangzhou Normal University
-- ============================================================================
update nad_university set
  total_students         = 32491,
  international_students = 450,
  faculty_count          = 3118,
  logo_image_url         = 'https://www.hznu.edu.cn/template/images/logo_03.png',
  cover_image_url        = 'https://www.hznu.edu.cn/upload/resources/image/2026/10/16/KE2KOR5KKE50OPPKEQ5XWRN1KQ5DE.jpg',
  accommodation_info     = 'International students live in dedicated on-campus apartments of different specifications. Rooms are equipped with a refrigerator, air conditioning, wireless internet and a private bathroom, and professional staff run daily management and services. Degree students pay for a single room once per academic year: RMB 7,000, 10,000 or 14,000 depending on the room (2025 fee schedule). Non-degree students are charged about RMB 40 to 45 per day, by semester or year. Fees change, so confirm the current schedule with the International Education College before applying.',
  nearby_info            = 'The university has three campuses in Hangzhou: Cangqian in Hangzhou Future Sci-Tech City, Yuhuangshan beside West Lake, and Xiasha on the bank of the Qiantang River. The address on the English site is 2318 Yuhangtang Road, Yuhang District, Hangzhou 311121.',
  update_by              = 'import-v106',
  update_time            = now()
where name = 'Hangzhou Normal University' and country = 'CN';

set @u := (select id from nad_university where name = 'Hangzhou Normal University' and country = 'CN');

insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, g.caption, g.sort_order from (
  select 1 as sort_order, 'Cangqian Campus' as caption,
         'https://www.hznu.edu.cn/upload/resources/image/2026/10/16/KE2KOR5KKE50OPPKEQ5XWRN1KQ5DE.jpg' as image_url
  union all select 2, 'Xiasha Campus',
         'https://www.hznu.edu.cn/upload/resources/image/2022/03/31/QRXKK3BYLA20OPNUEQ5XSQB1KM4XO.jpg'
  union all select 3, 'Yuhuangshan Campus: Fine Arts School',
         'https://www.hznu.edu.cn/upload/resources/image/2019/02/26/LA5XE3XCLA6XMPN1EQ5XMSNXKQ3XS.jpg'
) g
where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v106', now() from (
  select 1 as sort_order, 'School of Economics' as name, '经济学院' as name_cn
  union all select 2,  'School of Public Administration', '公共管理学院'
  union all select 3,  'Alibaba Business School', '阿里巴巴商学院'
  union all select 4,  'Shen Junru Law School', '沈钧儒法学院'
  union all select 5,  'School of Marxism', '马克思主义学院'
  union all select 6,  'Jing Hengyi School of Education', '经亨颐教育学院'
  union all select 7,  'School of Physical Education (Hangzhou Football Academy)', '体育学院（杭州足球学院）'
  union all select 8,  'School of Humanities', '人文学院'
  union all select 9,  'School of Foreign Languages', '外国语学院'
  union all select 10, 'School of Mathematics', '数学学院'
  union all select 11, 'School of Physics', '物理学院'
  union all select 12, 'School of Materials, Chemistry and Chemical Engineering', '材料与化学化工学院'
  union all select 13, 'School of Life and Environmental Sciences', '生命与环境科学学院'
  union all select 14, 'School of Information Science and Technology', '信息科学与技术学院'
  union all select 15, 'School of Engineering', '工学院'
  union all select 16, 'School of Basic Medical Sciences', '基础医学院'
  union all select 17, 'School of Public Health and Nursing', '公共卫生与护理学院'
  union all select 18, 'School of Pharmacy', '药学院'
  union all select 19, 'School of Clinical Medicine', '临床医学院'
  union all select 20, 'School of Stomatology', '口腔医学院'
  union all select 21, 'School of Music', '音乐学院'
  union all select 22, 'School of Fine Arts', '美术学院'
  union all select 23, 'School of Cultural Creativity and Media', '文化创意与传媒学院'
  union all select 24, 'Li Shutong School of Aesthetic Education (Shutong College)', '李叔同美育学院（叔同书院）'
  union all select 25, 'International Education College', '国际教育学院'
  union all select 26, 'Kharkiv Institute', '哈尔科夫学院'
) d where @u is not null;

-- ============================================================================
-- Hanjiang Normal University
-- ============================================================================
-- The official site publishes no international-student housing information, so accommodation_info stays empty.
update nad_university set
  total_students  = 18708,
  logo_image_url  = 'https://www.hjnu.edu.cn/images/HJNU_LOGO.jpg',
  nearby_info     = 'The university has two campuses: the Shiyan campus at No. 18 Beijing South Road, Maojian District, Shiyan (postcode 442000), and the Danjiangkou campus at No. 1 Xuefu Road, Danjiangkou. The school describes a garden-style campus with landscaping inspired by the Han River, in the reservoir area of the South-to-North Water Diversion Middle Route.',
  update_by       = 'import-v106',
  update_time     = now()
where name = 'Hanjiang Normal University' and country = 'CN';

set @u := (select id from nad_university where name = 'Hanjiang Normal University' and country = 'CN');

insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v106', now() from (
  select 1 as sort_order, 'School of Marxism' as name, '马克思主义学院' as name_cn
  union all select 2,  'School of Education', '教育学院'
  union all select 3,  'School of Economics and Management', '经济与管理学院'
  union all select 4,  'School of Literature and Media', '文学与传媒学院'
  union all select 5,  'School of History, Culture and Tourism', '历史文化与旅游学院'
  union all select 6,  'School of Foreign Languages', '外国语学院'
  union all select 7,  'School of Mathematics and Statistics', '数学与统计学院'
  union all select 8,  'School of Artificial Intelligence', '人工智能学院'
  union all select 9,  'School of Physics and Electronic Engineering', '物理与电子工程学院'
  union all select 10, 'School of Chemistry and Materials', '化学与材料学院'
  union all select 11, 'School of Ecology and Environment', '生态与环境学院'
  union all select 12, 'School of Arts', '艺术学院'
  union all select 13, 'School of Physical Education', '体育学院'
  union all select 14, 'Wudang Martial Arts School', '武当武术学院'
) d where @u is not null;

-- ============================================================================
-- Harbin Normal University (text only, see provenance above)
-- ============================================================================
update nad_university set
  accommodation_info = 'The International Students Dormitory offers single and double rooms. Listed rates per person per day: single room RMB 45 and double room RMB 25 for regular students, single room RMB 55 and double room RMB 35 for short-term students, with electricity and internet included. Rooms have a TV, private toilet and shower, refrigerator, internet access, closet and desk. Shared facilities include a study room, gym, multifunctional hall, meeting room, kitchen, laundry, leisure room and multimedia room. Source: CUCAS listing updated November 2024. Confirm current rates with the university.',
  nearby_info        = 'The university has two campuses in Harbin: Songbei campus at No. 1 Shida Road, Limin Economic Development Zone, and Jiangnan campus at No. 50 Hexing Road, Nangang District.',
  update_by          = 'import-v106',
  update_time        = now()
where name = 'Harbin Normal University' and country = 'CN';

set @u := (select id from nad_university where name = 'Harbin Normal University' and country = 'CN');

insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v106', now() from (
  select 1 as sort_order, 'School of Literature' as name, '文学院' as name_cn
  union all select 2,  'School of History and Culture', '历史文化学院'
  union all select 3,  'School of Teacher Education', '教师教育学院'
  union all select 4,  'School of Educational Science', '教育科学学院'
  union all select 5,  'School of Mathematical Sciences', '数学科学学院'
  union all select 6,  'School of Computer Science and Information Engineering', '计算机科学与信息工程学院'
  union all select 7,  'School of Physics and Electronic Engineering', '物理与电子工程学院'
  union all select 8,  'School of Chemistry and Chemical Engineering', '化学化工学院'
  union all select 9,  'School of Life Science and Technology', '生命科学与技术学院'
  union all select 10, 'School of Geographical Sciences', '地理科学学院'
  union all select 11, 'School of Sports Science', '体育科学学院'
  union all select 12, 'School of Marxism', '马克思主义学院'
  union all select 13, 'School of Law', '法学院'
  union all select 14, 'School of Western Languages', '西语学院'
  union all select 15, 'School of Eastern Languages', '东语学院'
  union all select 16, 'School of Slavic Languages', '斯拉夫语学院'
  union all select 17, 'School of Economics and Management', '经济与管理学院'
  union all select 18, 'School of Fine Arts', '美术学院'
  union all select 19, 'School of Music', '音乐学院'
  union all select 20, 'School of Communication', '传媒学院'
  union all select 21, 'International Education College', '国际教育学院'
) d where @u is not null;
