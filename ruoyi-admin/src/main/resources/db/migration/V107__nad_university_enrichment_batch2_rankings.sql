-- Catalog enrichment, batch 2, plus rankings from U.S. News for the universities of batch 1 and 2. DML only; rows stay as they are.
--
-- 1. Website: every university's website is now https://nadoumi.com/ (staff request). The previous official-site values
--    were the V54 / V90 seed values and are not preserved here.
-- 2. Rankings: the old seed ranking rows were marked "approximate -- verify" and are removed for the universities below.
--    They are replaced by U.S. News Best Global Universities 2026-2027 figures (overall, Asia, China, and subject ranks in
--    the top 250). U.S. News pages could not be opened directly from our network; the numbers were read from search
--    results that quote each university's U.S. News profile. A university with no row is unranked there, or its figure
--    could not be confirmed (Hanjiang, Harbin Normal, Jilin Jianzhu, Liaoning Petrochemical, Jiangxi, the two vocational
--    colleges). Staff can re-check any figure on usnews.com.
-- 3. Colleges, addresses and images come from each university's own site (checked 2026-10-19). Every image was downloaded
--    and looked at first, and each one is the university's own file (campus-scenery pages or school-emblem pages).
--    Images are linked by URL (legacy *_image_url columns / gallery image_url), not copied. Where a university only offers a
--    white header logo or no usable campus photos, nothing was added. Hunan University of Technology and Huzhou were not
--    touched beyond the website and rankings: the former's site cannot be reached from here, the latter's identity is
--    still unresolved (see V90 remark).
-- 4. Accommodation is not filled for these universities: none of the official sites states dormitory information.
-- 5. Department English names are plain translations of the official Chinese names.
-- 6. total_students corrected where the official profile gives a single total: Linyi 52000 (was 34500), Nanjing Forestry
--    40000 (34500), NJUPT 36000 (30000), Henan UT 40000 (33500), Jiangxi UT 40000 (38000), Mianyang Polytechnic 12000
--    (11000), Liaoning Petrochemical 17000 (16200).
--
-- Manual rollback: the previous website values and the removed ranking rows are in V54 / V90; delete the rows this adds with
--   delete from nad_department where create_by = 'import-v107';
--   delete from nad_university_ranking where source like 'US News%';
--   (images: clear logo_image_url / cover_image_url and delete the gallery rows of the listed universities)

update nad_university set website = 'https://nadoumi.com/';

delete r from nad_university_ranking r join nad_university u on u.id = r.university_id
  where u.country = 'CN' and u.name in ('Hangzhou Normal University', 'Hanjiang Normal University', 'Harbin Normal University', 'Hebei Normal University', 'Hebei University of Technology', 'Henan University of Technology', 'Hubei Normal University', 'Hubei University of Arts and Science', 'Hunan University of Technology', 'Jiangsu Vocational College of Medicine', 'Jiangxi University of Technology', 'Jilin Jianzhu University', 'Liaoning Petrochemical University', 'Linyi University', 'Mianyang Polytechnic', 'Nanjing Forestry University', 'Nanjing University of Posts and Telecommunications', 'Nantong University');

-- U.S. News rankings: Hangzhou Normal University
set @u := (select id from nad_university where name = 'Hangzhou Normal University' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 605, 2026, 'Tied rank, 2026-2027 edition, global score 45.7'),
 (@u, 'US News Best Global Universities (Asia)', 173, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 85, 2026, '2026-2027 edition'),
 (@u, 'US News: Materials Science', 207, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Physical Chemistry', 215, 2026, '2026-2027 subject ranking, tied rank');

-- U.S. News rankings: Hebei Normal University
set @u := (select id from nad_university where name = 'Hebei Normal University' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 1829, 2026, 'Tied rank, 2026-2027 edition, global score 23.8'),
 (@u, 'US News Best Global Universities (Asia)', 768, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 313, 2026, '2026-2027 edition');

-- U.S. News rankings: Hebei University of Technology
set @u := (select id from nad_university where name = 'Hebei University of Technology' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 1021, 2026, 'Tied rank, 2026-2027 edition, global score 37.2'),
 (@u, 'US News Best Global Universities (Asia)', 349, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 146, 2026, '2026-2027 edition'),
 (@u, 'US News: Civil Engineering', 112, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Chemical Engineering', 113, 2026, '2026-2027 subject ranking, tied rank'),
 (@u, 'US News: Condensed Matter Physics', 138, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Artificial Intelligence', 219, 2026, '2026-2027 subject ranking, tied rank');

-- U.S. News rankings: Henan University of Technology
set @u := (select id from nad_university where name = 'Henan University of Technology' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 1599, 2026, 'Tied rank, 2026-2027 edition, global score 27.2'),
 (@u, 'US News Best Global Universities (Asia)', 634, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 257, 2026, '2026-2027 edition'),
 (@u, 'US News: Polymer Science', 121, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Food Science and Technology', 107, 2026, '2026-2027 subject ranking, tied rank'),
 (@u, 'US News: Agricultural Sciences', 201, 2026, '2026-2027 subject ranking, tied rank');

-- U.S. News rankings: Hubei Normal University
set @u := (select id from nad_university where name = 'Hubei Normal University' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 1957, 2026, 'Tied rank, 2026-2027 edition, global score 21.5'),
 (@u, 'US News Best Global Universities (Asia)', 845, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 339, 2026, '2026-2027 edition');

-- U.S. News rankings: Hubei University of Arts and Science
set @u := (select id from nad_university where name = 'Hubei University of Arts and Science' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 1687, 2026, 'Tied rank, 2026-2027 edition, global score 26'),
 (@u, 'US News Best Global Universities (Asia)', 682, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 275, 2026, '2026-2027 edition');

-- U.S. News rankings: Hunan University of Technology
set @u := (select id from nad_university where name = 'Hunan University of Technology' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 1481, 2026, 'Tied rank, 2026-2027 edition, global score 29.3'),
 (@u, 'US News Best Global Universities (Asia)', 574, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 239, 2026, '2026-2027 edition');

-- U.S. News rankings: Linyi University
set @u := (select id from nad_university where name = 'Linyi University' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 1332, 2026, 'Tied rank, 2026-2027 edition, global score 31.9'),
 (@u, 'US News Best Global Universities (Asia)', 500, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 206, 2026, '2026-2027 edition'),
 (@u, 'US News: Mathematics', 111, 2026, '2026-2027 subject ranking');

-- U.S. News rankings: Nanjing Forestry University
set @u := (select id from nad_university where name = 'Nanjing Forestry University' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 386, 2026, 'Tied rank, 2026-2027 edition, global score 52.8'),
 (@u, 'US News Best Global Universities (Asia)', 98, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 57, 2026, '2026-2027 edition'),
 (@u, 'US News: Polymer Science', 1, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Chemical Engineering', 23, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Environmental Engineering', 21, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Agricultural Sciences', 46, 2026, '2026-2027 subject ranking');

-- U.S. News rankings: Nanjing University of Posts and Telecommunications
set @u := (select id from nad_university where name = 'Nanjing University of Posts and Telecommunications' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 688, 2026, 'Tied rank, 2026-2027 edition'),
 (@u, 'US News Best Global Universities (Asia)', 207, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 98, 2026, '2026-2027 edition'),
 (@u, 'US News: Electrical & Electronic Eng.', 46, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Artificial Intelligence', 98, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Computer Science', 107, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Optics', 103, 2026, '2026-2027 subject ranking');

-- U.S. News rankings: Nantong University
set @u := (select id from nad_university where name = 'Nantong University' and country = 'CN');
insert into nad_university_ranking (university_id, source, rank_position, rank_year, note) values
 (@u, 'US News Best Global Universities', 1081, 2026, 'Tied rank, 2026-2027 edition, global score 36.3'),
 (@u, 'US News Best Global Universities (Asia)', 374, 2026, '2026-2027 edition'),
 (@u, 'US News Best Global Universities (China)', 155, 2026, '2026-2027 edition'),
 (@u, 'US News: Artificial Intelligence', 119, 2026, '2026-2027 subject ranking'),
 (@u, 'US News: Biotechnology & Microbiology', 186, 2026, '2026-2027 subject ranking');

-- Hebei Normal University
set @u := (select id from nad_university where name = 'Hebei Normal University' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Teacher Education' as name, '教师教育学院' as name_cn
  union all select 2, 'School of Marxism', '马克思主义学院'
  union all select 3, 'School of Education', '教育学院'
  union all select 4, 'School of Literature', '文学院'
  union all select 5, 'School of History and Culture', '历史文化学院'
  union all select 6, 'School of Foreign Languages', '外国语学院'
  union all select 7, 'School of Music', '音乐学院'
  union all select 8, 'School of Fine Arts and Design', '美术与设计学院'
  union all select 9, 'School of Journalism and Communication', '新闻传播学院'
  union all select 10, 'Business School', '商学院'
  union all select 11, 'School of Law, Politics and Public Administration', '法政与公共管理学院'
  union all select 12, 'School of Physics', '物理学院'
  union all select 13, 'School of Chemistry and Materials Science', '化学与材料科学学院'
  union all select 14, 'School of Life Sciences', '生命科学学院'
  union all select 15, 'School of Geographical Sciences', '地理科学学院'
  union all select 16, 'School of Physical Education', '体育学院'
  union all select 17, 'School of Computer Science and Cyberspace Security', '计算机与网络空间安全学院'
  union all select 18, 'School of International Cultural Exchange', '国际文化交流学院'
) d where @u is not null;
update nad_university set
  cover_image_url = coalesce(cover_image_url, 'https://www.hebtu.edu.cn/resources/40/202504/1F8C477E6C6340D8A5681922161AEB74.jpg'),
  nearby_info = coalesce(nearby_info, 'The university has two campuses in Shijiazhuang: Yuhua campus at No. 20 South Second Ring East Road and Hongqi campus at No. 469 Hongqi Street. Its new campus covers 1,829 mu.'),
  update_by = 'import-v107',
  update_time = now()
where id = @u;
insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, 'Campus view', g.sort_order from (
  select 1 as sort_order, 'https://www.hebtu.edu.cn/resources/40/202504/CA6613968BB94D40BA91D8D2DD82D867.jpg' as image_url
  union all select 2, 'https://www.hebtu.edu.cn/resources/40/202504/28E8D9D19C0A4ACEB7F2817A50E92CA5.jpg'
  union all select 3, 'https://www.hebtu.edu.cn/resources/40/202504/632634E627E347EFA30885703B4CDBD8.jpg'
  union all select 4, 'https://www.hebtu.edu.cn/resources/40/202504/80BFA0DE2092462BA1558E9B1EB038C9.jpg'
) g where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

-- Hebei University of Technology
set @u := (select id from nad_university where name = 'Hebei University of Technology' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Electrical Engineering' as name, '电气工程学院' as name_cn
  union all select 2, 'School of Materials Science and Engineering', '材料科学与工程学院'
  union all select 3, 'School of Chemical Engineering', '化工学院'
  union all select 4, 'School of Mechanical Engineering', '机械工程学院'
  union all select 5, 'School of Economics and Management', '经济管理学院'
  union all select 6, 'School of Civil Engineering and Transportation', '土木与交通学院'
  union all select 7, 'School of Electronic and Information Engineering', '电子信息工程学院'
  union all select 8, 'School of Artificial Intelligence and Data Science', '人工智能与数据科学学院'
  union all select 9, 'School of Energy and Environmental Engineering', '能源与环境工程学院'
  union all select 10, 'School of Life Science and Health Engineering', '生命科学与健康工程学院'
  union all select 11, 'School of Science', '理学院'
  union all select 12, 'School of Architecture and Art Design', '建筑与艺术设计学院'
  union all select 13, 'School of Marxism', '马克思主义学院'
  union all select 14, 'School of Foreign Languages', '外国语学院'
  union all select 15, 'School of Humanities and Law', '人文与法律学院'
  union all select 16, 'International Education College', '国际教育学院'
  union all select 17, 'Arizona Institute of Technology, Hebei University of Technology', '河北工业大学亚利桑那工业学院'
  union all select 18, 'Liverpool College', '利物浦学院'
) d where @u is not null;
update nad_university set
  nearby_info = coalesce(nearby_info, 'The main address is No. 5340 Xiping Road, Beichen District, Tianjin (postcode 300401). The university also runs a Finland campus together with Lappeenranta-Lahti University of Technology.'),
  update_by = 'import-v107',
  update_time = now()
where id = @u;
insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, 'Campus view', g.sort_order from (
  select 1 as sort_order, 'https://www.hebut.edu.cn/new/images/simg01.jpg' as image_url
  union all select 2, 'https://www.hebut.edu.cn/new/images/simg02.jpg'
  union all select 3, 'https://www.hebut.edu.cn/new/images/simg03.jpg'
  union all select 4, 'https://www.hebut.edu.cn/new/images/simg04.jpg'
) g where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

-- Henan University of Technology
set @u := (select id from nad_university where name = 'Henan University of Technology' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Grain, Oil and Food' as name, '粮油食品学院' as name_cn
  union all select 2, 'School of Grain and Strategic Reserves', '粮食和物资储备学院'
  union all select 3, 'School of Mechanical and Electrical Engineering', '机电工程学院'
  union all select 4, 'School of Civil Engineering', '土木工程学院'
  union all select 5, 'School of Information Science and Engineering', '信息科学与工程学院'
  union all select 6, 'School of Artificial Intelligence and Big Data', '人工智能与大数据学院'
  union all select 7, 'School of Chemistry and Chemical Engineering', '化学化工学院'
  union all select 8, 'School of Bioengineering', '生物工程学院'
  union all select 9, 'School of Materials Science and Engineering', '材料科学与工程学院'
  union all select 10, 'School of Electrical Engineering', '电气工程学院'
  union all select 11, 'School of Environmental Engineering', '环境工程学院'
  union all select 12, 'School of Management', '管理学院'
  union all select 13, 'School of Economics and Trade', '经济贸易学院'
  union all select 14, 'School of Foreign Languages', '外语学院'
  union all select 15, 'School of Mathematics and Statistics', '数学与统计学院'
  union all select 16, 'School of Physics and New Energy', '物理与新能源学院'
  union all select 17, 'School of Design and Art', '设计艺术学院'
  union all select 18, 'School of Journalism and Communication', '新闻与传播学院'
  union all select 19, 'School of Law', '法学院'
  union all select 20, 'School of Marxism', '马克思主义学院'
  union all select 21, 'School of Physical Education', '体育学院'
  union all select 22, 'International Education College', '国际教育学院'
  union all select 23, 'Mingde College', '明德书院'
) d where @u is not null;
update nad_university set
  logo_image_url = coalesce(logo_image_url, 'https://www.haut.edu.cn/images/mzw/pic-b4.png'),
  cover_image_url = coalesce(cover_image_url, 'https://www.haut.edu.cn/images/mzw/pic-b2.jpg'),
  nearby_info = coalesce(nearby_info, 'The university has three campuses in Zhengzhou, on Lianhua Street, Songshan Road and Zhongyuan Road. The main address is No. 100 Lianhua Street, Zhengzhou High-tech Zone (postcode 450001), and the campuses cover 2,365 mu.'),
  total_students = 40000,
  update_by = 'import-v107',
  update_time = now()
where id = @u;

-- Hubei Normal University
set @u := (select id from nad_university where name = 'Hubei Normal University' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Economics, Management and Law' as name, '经济管理与法学院' as name_cn
  union all select 2, 'School of Marxism', '马克思主义学院'
  union all select 3, 'School of Educational Science', '教育科学学院'
  union all select 4, 'School of Physical Education', '体育学院'
  union all select 5, 'School of Literature', '文学院'
  union all select 6, 'School of Foreign Languages', '外国语学院'
  union all select 7, 'School of History and Culture', '历史文化学院'
  union all select 8, 'School of Mathematics and Statistics', '数学与统计学院'
  union all select 9, 'School of Physics and Electronic Science', '物理与电子科学学院'
  union all select 10, 'School of Chemistry and Chemical Engineering', '化学化工学院'
  union all select 11, 'School of Urban and Environmental Sciences', '城市与环境学院'
  union all select 12, 'School of Life Sciences', '生命科学学院'
  union all select 13, 'School of Electrical Engineering and Automation', '电气工程与自动化学院'
  union all select 14, 'School of Artificial Intelligence and Computer Science', '人工智能与计算机学院'
  union all select 15, 'School of Music and Dance', '音乐舞蹈学院'
  union all select 16, 'School of Fine Arts', '美术学院'
  union all select 17, 'School of Materials Science and Engineering', '材料科学与工程学院'
) d where @u is not null;
update nad_university set
  cover_image_url = coalesce(cover_image_url, 'https://www.hbnu.edu.cn/_upload/article/images/22/e0/4557cdd24ae2b9b3dff11cd8778b/5777ac65-e0cd-4c6e-be5a-cd845a84ecdb.jpg'),
  nearby_info = coalesce(nearby_info, 'The campus is at No. 11 Cihu Road, Huangshi, Hubei (postcode 435002). The planned site covers 2,002 mu, with more than 600,000 square metres of buildings.'),
  update_by = 'import-v107',
  update_time = now()
where id = @u;
insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, 'Campus view', g.sort_order from (
  select 1 as sort_order, 'https://www.hbnu.edu.cn/_upload/article/images/22/e0/4557cdd24ae2b9b3dff11cd8778b/64bddb7e-3c49-41b3-ba90-5c671ea29a00.jpg' as image_url
  union all select 2, 'https://www.hbnu.edu.cn/_upload/article/images/22/e0/4557cdd24ae2b9b3dff11cd8778b/4224b678-d51c-4f2d-b640-bea9912c9c5d.jpg'
  union all select 3, 'https://www.hbnu.edu.cn/_upload/article/images/22/e0/4557cdd24ae2b9b3dff11cd8778b/bbc39fff-3c0a-4ab4-b585-80cb22f7e2e0.jpg'
  union all select 4, 'https://www.hbnu.edu.cn/_upload/article/images/22/e0/4557cdd24ae2b9b3dff11cd8778b/fd16fb6c-c38a-4987-8cda-ff2aaf279666.jpg'
) g where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

-- Hubei University of Arts and Science
set @u := (select id from nad_university where name = 'Hubei University of Arts and Science' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Mechanical Engineering' as name, '机械工程学院' as name_cn
  union all select 2, 'School of Automotive and Transportation Engineering', '汽车与交通工程学院'
  union all select 3, 'School of Civil Engineering and Architecture', '土木工程与建筑学院'
  union all select 4, 'School of Basic Medical Sciences', '基础医学院'
  union all select 5, 'School of Clinical Medicine', '临床医学院'
  union all select 6, 'School of Computer Engineering', '计算机工程学院'
  union all select 7, 'School of Teacher Education', '教师教育学院'
  union all select 8, 'School of Economics and Management', '经济管理学院'
  union all select 9, 'School of Marxism', '马克思主义学院'
  union all select 10, 'School of Fine Arts', '美术学院'
  union all select 11, 'School of Food Science and Chemical Engineering', '食品科学与化学工程学院'
  union all select 12, 'School of Mathematics and Statistics', '数学与统计学院'
  union all select 13, 'School of Physical Education', '体育学院'
  union all select 14, 'School of Foreign Languages', '外国语学院'
  union all select 15, 'School of Literature and Media', '文学与传媒学院'
  union all select 16, 'School of Physics and Electronic Engineering', '物理与电子工程学院'
  union all select 17, 'School of Music and Dance', '音乐与舞蹈学院'
  union all select 18, 'School of Politics and Law', '政法学院'
  union all select 19, 'School of Resources, Environment and Tourism', '资源环境与旅游学院'
) d where @u is not null;
update nad_university set
  cover_image_url = coalesce(cover_image_url, 'https://www.hbuas.edu.cn/__local/E/09/8E/7559122EA6ADC2F7C03CFF93E90_06871347_7065E.jpg'),
  nearby_info = coalesce(nearby_info, 'The main address is No. 296 Longzhong Road, Xiangyang, Hubei (postcode 441053). The university has Longzhong, Wolong and Dongjin campuses, covering more than 3,000 mu.'),
  update_by = 'import-v107',
  update_time = now()
where id = @u;
insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, 'Campus view', g.sort_order from (
  select 1 as sort_order, 'https://www.hbuas.edu.cn/__local/8/57/94/C97BD165A9989DE0A3E121BA391_76B9344F_A48FC.jpg' as image_url
  union all select 2, 'https://www.hbuas.edu.cn/__local/1/0C/A9/8528F0FA7B3C9E6B6158347617D_07D30BB1_94BB2.jpg'
  union all select 3, 'https://www.hbuas.edu.cn/__local/C/24/6B/F6CCF59F1A474B1CC89882CC312_78F8BDF8_7C049.jpg'
  union all select 4, 'https://www.hbuas.edu.cn/__local/9/7C/38/8AFEFE9F9FBFBF242581DC3EB8A_22AC21E7_DC748.jpg'
) g where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

-- Jiangsu Vocational College of Medicine
set @u := (select id from nad_university where name = 'Jiangsu Vocational College of Medicine' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Medicine' as name, '医学院' as name_cn
  union all select 2, 'School of Nursing', '护理学院'
  union all select 3, 'School of Pharmacy', '药学院'
  union all select 4, 'School of Traditional Chinese Medicine', '中医药学院'
  union all select 5, 'School of Rehabilitation Medicine', '康复医学院'
  union all select 6, 'School of Medical-Elderly Care Integration', '医养结合学院'
  union all select 7, 'School of Digital Intelligence Medical Engineering', '数智医学工程学院'
  union all select 8, 'International Education College', '国际教育学院'
  union all select 9, 'School of Marxism', '马克思主义学院'
) d where @u is not null;
update nad_university set
  logo_image_url = coalesce(logo_image_url, 'https://www.jsmc.edu.cn/_upload/article/images/b0/f2/fdfb020749eabf1eba5e2df49481/d4418f58-dfa5-445b-8c06-7527e3328df4.png'),
  nearby_info = coalesce(nearby_info, 'The college is at No. 283 Jiefang South Road, Yancheng, Jiangsu (postcode 224005), and covers 843.3 mu. It describes Yancheng as the wetland capital of the East.'),
  update_by = 'import-v107',
  update_time = now()
where id = @u;

-- Jiangxi University of Technology
set @u := (select id from nad_university where name = 'Jiangxi University of Technology' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Medicine' as name, '医学院' as name_cn
  union all select 2, 'School of Economics and Management', '经济管理学院'
  union all select 3, 'School of Intelligent Engineering', '智能工程学院'
  union all select 4, 'School of Information Engineering', '信息工程学院'
  union all select 5, 'School of Big Data', '大数据学院'
  union all select 6, 'School of Art and Design', '艺术设计学院'
  union all select 7, 'School of Music and Dance', '音乐舞蹈学院'
  union all select 8, 'School of Culture and Media', '文化与传媒学院'
  union all select 9, 'Boya International College', '博雅国际学院'
  union all select 10, 'School of Marxism', '马克思主义学院'
  union all select 11, 'School of Physical Education and Education', '体育与教育学院'
  union all select 12, 'School of Frontier Interdisciplinary Studies', '前沿交叉学院'
  union all select 13, 'School of Robotics and Automation', '机器人与自动化学院'
) d where @u is not null;
update nad_university set
  cover_image_url = coalesce(cover_image_url, 'https://www.jxut.edu.cn/__local/0/F2/5A/2CED69B47FEA1D0D834B1799C6B_3C7B6847_14816.jpg'),
  nearby_info = coalesce(nearby_info, 'The campus is at No. 115 Ziyang Avenue, High-tech Zone, Nanchang (postcode 330098), and covers nearly 2,000 mu.'),
  total_students = 40000,
  update_by = 'import-v107',
  update_time = now()
where id = @u;
insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, 'Campus view', g.sort_order from (
  select 1 as sort_order, 'https://www.jxut.edu.cn/__local/3/C2/DD/94593300F4960F85C2CD2C9376F_EDF60A4A_57020.jpg' as image_url
  union all select 2, 'https://www.jxut.edu.cn/__local/0/19/AA/FCE580D52EEEC0A247F02839915_BDE89282_1733E.jpg'
  union all select 3, 'https://www.jxut.edu.cn/__local/8/42/6F/B738DAFC99912AE43A90116C3B3_745D4DA4_2524F.jpg'
  union all select 4, 'https://www.jxut.edu.cn/__local/6/7A/65/D8DD313D37E033CE9040D16CD03_113F40B2_2258E.jpg'
) g where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

-- Jilin Jianzhu University
set @u := (select id from nad_university where name = 'Jilin Jianzhu University' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Architecture and Planning' as name, '建筑与规划学院' as name_cn
  union all select 2, 'School of Civil Engineering', '土木工程学院'
  union all select 3, 'School of Municipal and Environmental Engineering', '市政与环境工程学院'
  union all select 4, 'School of Materials Science and Engineering', '材料科学与工程学院'
  union all select 5, 'School of Economics and Management', '经济与管理学院'
  union all select 6, 'School of Transportation Science and Engineering', '交通科学与工程学院'
  union all select 7, 'School of Electrical and Computer Engineering', '电气与计算机学院'
  union all select 8, 'School of Surveying, Mapping and Exploration Engineering', '测绘与勘查工程学院'
  union all select 9, 'School of Art and Design', '艺术设计学院'
  union all select 10, 'School of Calligraphy', '书法学院'
  union all select 11, 'School of Emergency Science and Engineering', '应急科学与工程学院'
  union all select 12, 'School of Modern Industry', '现代产业学院'
  union all select 13, 'School of Marxism', '马克思主义学院'
) d where @u is not null;
update nad_university set
  logo_image_url = coalesce(logo_image_url, 'https://www.jlju.edu.cn/__local/B/CD/5E/2BD2F6F5A56D808979461443CE5_CE09D45B_886C.jpg'),
  cover_image_url = coalesce(cover_image_url, 'https://www.jlju.edu.cn/__local/5/33/76/06FD0F037FD6248BEA9E62685F8_A51FF673_6B0F0.jpg'),
  nearby_info = coalesce(nearby_info, 'The campus is in the Jingyuetan National High-tech Industrial Development Zone in Changchun, at No. 5088 Xincheng Avenue. It covers 800,200 square metres, with 377,700 square metres of buildings.'),
  update_by = 'import-v107',
  update_time = now()
where id = @u;
insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, 'Campus view', g.sort_order from (
  select 1 as sort_order, 'https://www.jlju.edu.cn/__local/7/27/4C/58AF8D98CE5AAD5ABCBE036293E_49BDBCA9_4D6C2.jpg' as image_url
  union all select 2, 'https://www.jlju.edu.cn/__local/5/5D/2F/454D2FA40126CAC127DE0FA2413_6C34257F_76DF3.jpg'
) g where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

-- Liaoning Petrochemical University
set @u := (select id from nad_university where name = 'Liaoning Petrochemical University' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Petrochemical Engineering' as name, '石油化工学院' as name_cn
  union all select 2, 'School of Information and Control Engineering', '信息与控制工程学院'
  union all select 3, 'School of Mechanical Engineering', '机械工程学院'
  union all select 4, 'School of Economics and Management', '经济管理学院'
  union all select 5, 'School of Science', '理学院'
  union all select 6, 'School of Marxism', '马克思主义学院'
  union all select 7, 'School of Foreign Languages', '外国语学院'
  union all select 8, 'School of Physical Education', '体育学院'
  union all select 9, 'School of Green Mining and Resources Engineering', '绿色矿业与资源工程学院'
  union all select 10, 'School of Aesthetic Education and Design', '美育与设计学院'
  union all select 11, 'School of New Energy and New Materials', '新能源与新材料学院'
  union all select 12, 'School of Environmental and Safety Engineering', '环境与安全工程学院'
  union all select 13, 'International Education College', '国际教育学院'
  union all select 14, 'School of Ethnic Minority Education', '民族教育学院'
  union all select 15, 'Excellence Engineers College', '卓越工程师学院'
) d where @u is not null;
update nad_university set
  cover_image_url = coalesce(cover_image_url, 'https://www.lnpu.edu.cn/__local/5/64/AB/F946840F246CBEA4268942D784C_58F6822D_83BB3.jpg'),
  nearby_info = coalesce(nearby_info, 'The campus is at No. 1 Dandong Road West Section, Wanghua District, Fushun, Liaoning (postcode 113001), and covers about 1.26 million square metres.'),
  total_students = 17000,
  update_by = 'import-v107',
  update_time = now()
where id = @u;
insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, 'Campus view', g.sort_order from (
  select 1 as sort_order, 'https://www.lnpu.edu.cn/__local/2/83/64/0B276087E097BCFBD9389A9C3F6_9993C469_605E3.jpg' as image_url
  union all select 2, 'https://www.lnpu.edu.cn/__local/6/B6/02/4D042BFFF6637BDA3BB7EF329BE_2F837926_17580.png'
) g where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

-- Linyi University
set @u := (select id from nad_university where name = 'Linyi University' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Marxism' as name, '马克思主义学院' as name_cn
  union all select 2, 'Business School', '商学院'
  union all select 3, 'School of Logistics', '物流学院'
  union all select 4, 'School of Law', '法学院'
  union all select 5, 'School of Education', '教育学院'
  union all select 6, 'School of Sports and Health', '体育与健康学院'
  union all select 7, 'School of Music', '音乐学院'
  union all select 8, 'School of Fine Arts', '美术学院'
  union all select 9, 'School of Literature', '文学院'
  union all select 10, 'School of Foreign Languages', '外国语学院'
  union all select 11, 'School of Media', '传媒学院'
  union all select 12, 'School of History and Culture', '历史文化学院'
  union all select 13, 'School of Mathematics and Statistics', '数学与统计学院'
  union all select 14, 'School of Physics and Electronic Engineering', '物理与电子工程学院'
  union all select 15, 'School of Chemistry and Chemical Engineering', '化学化工学院'
  union all select 16, 'School of Medicine', '医学院'
  union all select 17, 'School of Mechanical and Vehicle Engineering', '机械与车辆工程学院'
  union all select 18, 'School of Materials Science and Engineering', '材料科学与工程学院'
  union all select 19, 'School of Automation and Electrical Engineering', '自动化与电气工程学院'
  union all select 20, 'School of Information Science and Engineering', '信息科学与工程学院'
  union all select 21, 'School of Civil Engineering and Architecture', '土木工程与建筑学院'
  union all select 22, 'School of Resources and Environment', '资源环境学院'
  union all select 23, 'School of Life Sciences', '生命科学学院'
  union all select 24, 'School of Agriculture and Forestry Sciences', '农林科学学院'
  union all select 25, 'International College of Bioresource Applications', '国际生物资源应用学院'
  union all select 26, 'School of Rural Revitalization', '乡村振兴学院'
) d where @u is not null;
update nad_university set
  logo_image_url = coalesce(logo_image_url, 'https://www.lyu.edu.cn/_upload/article/images/f5/06/302a99884efca8846cb02b261cb6/a167f79d-e63b-412a-bbd7-9c0bb3de0616.png'),
  nearby_info = coalesce(nearby_info, 'The main campus is on the west side of the north section of Gongye Avenue, Lanshan District, Linyi, Shandong (postcode 276000), and covers about 6,000 mu. The university also has Yishui and Fei County campuses.'),
  total_students = 52000,
  update_by = 'import-v107',
  update_time = now()
where id = @u;

-- Mianyang Polytechnic
set @u := (select id from nad_university where name = 'Mianyang Polytechnic' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Marxism' as name, '马克思主义学院' as name_cn
  union all select 2, 'School of Public Education', '公共教育学院'
  union all select 3, 'School of Materials Engineering', '材料工程学院'
  union all select 4, 'School of Architectural Engineering', '建筑工程学院'
  union all select 5, 'School of Electronic Information', '电子信息学院'
  union all select 6, 'School of Artificial Intelligence', '人工智能学院'
  union all select 7, 'School of Intelligent Manufacturing', '智能制造学院'
  union all select 8, 'School of Electrical Engineering', '电气工程学院'
  union all select 9, 'School of Finance, Economics and Commerce', '财经商贸学院'
  union all select 10, 'School of Culture and Tourism', '文化旅游学院'
  union all select 11, 'School of Art and Design', '艺术设计学院'
) d where @u is not null;
update nad_university set
  nearby_info = coalesce(nearby_info, 'The college is at No. 6 North Section of Xinyi Ring Road, Mianyang, Sichuan, and covers more than 800 mu.'),
  total_students = 12000,
  update_by = 'import-v107',
  update_time = now()
where id = @u;

-- Nanjing Forestry University
set @u := (select id from nad_university where name = 'Nanjing Forestry University' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Forestry and Grassland' as name, '林草学院' as name_cn
  union all select 2, 'School of Materials Science and Engineering', '材料科学与工程学院'
  union all select 3, 'School of Chemical Engineering', '化学工程学院'
  union all select 4, 'School of Mechanical and Electronic Engineering', '机械电子工程学院'
  union all select 5, 'School of Civil Engineering', '土木工程学院'
  union all select 6, 'School of Economics and Management', '经济管理学院'
  union all select 7, 'School of Humanities and Social Sciences', '人文社会科学学院'
  union all select 8, 'School of Information Science and Technology', '信息科学技术学院'
  union all select 9, 'School of Landscape Architecture', '风景园林学院'
  union all select 10, 'School of Science', '理学院'
  union all select 11, 'School of Foreign Languages', '外国语学院'
  union all select 12, 'School of Art and Design', '艺术设计学院'
  union all select 13, 'School of Furnishings and Industrial Design', '家居与工业设计学院'
  union all select 14, 'School of Light Industry and Food', '轻工与食品学院'
  union all select 15, 'School of Automotive and Transportation Engineering', '汽车与交通工程学院'
  union all select 16, 'School of Ecology and Environment', '生态与环境学院'
  union all select 17, 'School of Marxism', '马克思主义学院'
  union all select 18, 'School of Life Sciences', '生命科学学院'
  union all select 19, 'International Education College', '国际教育学院'
  union all select 20, 'Zhilin International College', '智林国际学院'
) d where @u is not null;
update nad_university set
  cover_image_url = coalesce(cover_image_url, 'https://www.njfu.edu.cn/DFS//network/c1153/i201822/161277772975937dhng.jpg'),
  nearby_info = coalesce(nearby_info, 'The university has the Nanjing Xinzhuang campus, the Baima campus and the Huai''an campus, covering more than 10,000 mu in total. The Nanjing address is No. 159 Longpan Road.'),
  total_students = 40000,
  update_by = 'import-v107',
  update_time = now()
where id = @u;
insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, 'Campus view', g.sort_order from (
  select 1 as sort_order, 'https://www.njfu.edu.cn/DFS//network/c1153/i201822/1612777729721fpm38i.jpg' as image_url
  union all select 2, 'https://www.njfu.edu.cn/DFS//network/c1153/i201822/16127777299339vysec.jpg'
  union all select 3, 'https://www.njfu.edu.cn/DFS//network/c1153/i201822/16127777300396m5hy8.jpg'
) g where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

-- Nanjing University of Posts and Telecommunications
set @u := (select id from nad_university where name = 'Nanjing University of Posts and Telecommunications' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Communication and Information Engineering' as name, '通信与信息工程学院' as name_cn
  union all select 2, 'School of Electronic and Optical Engineering', '电子与光学工程学院'
  union all select 3, 'School of Integrated Circuit Science and Engineering', '集成电路科学与工程学院'
  union all select 4, 'School of Computer Science', '计算机学院'
  union all select 5, 'School of Automation', '自动化学院'
  union all select 6, 'School of Artificial Intelligence', '人工智能学院'
  union all select 7, 'School of Materials Science and Engineering', '材料科学与工程学院'
  union all select 8, 'School of Chemistry and Life Sciences', '化学与生命科学学院'
  union all select 9, 'School of Internet of Things', '物联网学院'
  union all select 10, 'School of Science', '理学院'
  union all select 11, 'School of Modern Posts', '现代邮政学院'
  union all select 12, 'School of Digital Media and Design Art', '数字媒体与设计艺术学院'
  union all select 13, 'School of Management', '管理学院'
  union all select 14, 'School of Economics', '经济学院'
  union all select 15, 'School of Marxism', '马克思主义学院'
  union all select 16, 'School of Sociology and Population Studies', '社会与人口学院'
  union all select 17, 'School of Foreign Languages', '外国语学院'
  union all select 18, 'School of Educational Science and Technology', '教育科学与技术学院'
  union all select 19, 'Bell Honors College', '贝尔英才学院'
  union all select 20, 'School of Overseas Education', '海外教育学院'
  union all select 21, 'Portland College', '波特兰学院'
  union all select 22, 'European Cyprus College', '欧洲塞浦路斯学院'
  union all select 23, 'School of Applied Technology', '应用技术学院'
) d where @u is not null;
update nad_university set
  logo_image_url = coalesce(logo_image_url, 'https://www.njupt.edu.cn/_upload/tpl/0d/78/3448/template3448/image/logos.png'),
  cover_image_url = coalesce(cover_image_url, 'https://www.njupt.edu.cn/_upload/article/images/84/23/5b1137e540feac3eb9f211044fac/3de17e75-6d0b-461a-8bbf-3b44708a31d3.jpg'),
  nearby_info = coalesce(nearby_info, 'The Xianlin campus is at No. 9 Wenyuan Road, Xianlin University Town (postcode 210023). The Sanpailou campus is at No. 66 Xinmofan Ma Road (postcode 210003). The Suojincun campus is at No. 177 Longpan Road (postcode 210042). All are in Nanjing.'),
  total_students = 36000,
  update_by = 'import-v107',
  update_time = now()
where id = @u;
insert into nad_university_gallery (university_id, image_url, caption, sort_order)
select @u, g.image_url, 'Campus view', g.sort_order from (
  select 1 as sort_order, 'https://www.njupt.edu.cn/_upload/article/images/1f/55/ca74b8a44c1c94a98fbdaeb8232c/feeccb8f-aaa2-42a3-8d44-9d9ceafeec3f.jpg' as image_url
  union all select 2, 'https://www.njupt.edu.cn/_upload/article/images/4f/f3/f5432c7343268ef99013cb57ceb4/2a379af7-dc6a-469a-adcd-d1c03a331fa0.jpg'
  union all select 3, 'https://www.njupt.edu.cn/_upload/article/images/a6/62/bd89f64a4886ba9e0853776303a0/167ed05e-4702-40ea-ba7d-f02d2eca3824.jpg'
  union all select 4, 'https://www.njupt.edu.cn/_upload/article/images/28/b5/5ce418e04b4484686357c3ce4d12/8b43dfab-114d-4b14-b625-fdc9e08876e0.jpg'
) g where @u is not null
  and not exists (select 1 from nad_university_gallery x where x.university_id = @u);

-- Nantong University
set @u := (select id from nad_university where name = 'Nantong University' and country = 'CN');
insert ignore into nad_department (university_id, name, name_cn, sort_order, create_by, create_time)
select @u, d.name, d.name_cn, d.sort_order, 'import-v107', now() from (
  select 1 as sort_order, 'School of Literature' as name, '文学院' as name_cn
  union all select 2, 'School of History and Culture', '历史文化学院'
  union all select 3, 'School of Mathematics and Statistics', '数学与统计学院'
  union all select 4, 'School of Physical Science and Technology', '物理科学与技术学院'
  union all select 5, 'School of Marxism', '马克思主义学院'
  union all select 6, 'Business School', '商学院'
  union all select 7, 'School of Educational Science', '教育科学学院'
  union all select 8, 'School of Teacher Education', '教师教育学院'
  union all select 9, 'School of Foreign Languages', '外国语学院'
  union all select 10, 'School of Chemistry and Chemical Engineering', '化学化工学院'
  union all select 11, 'School of Life Sciences', '生命科学学院'
  union all select 12, 'School of Mechanical Engineering', '机械工程学院'
  union all select 13, 'School of Information Science and Technology', '信息科学技术学院'
  union all select 14, 'School of Microelectronics', '微电子学院'
  union all select 15, 'School of Artificial Intelligence and Computer Science', '人工智能与计算机学院'
  union all select 16, 'School of Electrical Engineering and Automation', '电气与自动化学院'
  union all select 17, 'School of Textiles and Apparel', '纺织服装学院'
  union all select 18, 'School of Medicine', '医学院'
  union all select 19, 'School of Nursing and Rehabilitation', '护理与康复学院'
  union all select 20, 'School of Public Health', '公共卫生学院'
  union all select 21, 'School of Sports Science', '体育科学学院'
  union all select 22, 'Zhiyun Football Academy', '支云足球学院'
  union all select 23, 'School of Arts', '艺术学院'
  union all select 24, 'School of Geographical Sciences', '地理科学学院'
  union all select 25, 'School of Transportation and Civil Engineering', '交通与土木工程学院'
  union all select 26, 'School of Pharmacy', '药学院'
  union all select 27, 'International Education College', '国际教育学院'
  union all select 28, 'Zhang Jian College', '张謇学院'
  union all select 29, 'School of Innovation and Entrepreneurship Education', '创新创业教育学院'
  union all select 30, 'Siena Future Biotechnology College', '锡耶纳未来生物技术学院'
) d where @u is not null;
update nad_university set
  logo_image_url = coalesce(logo_image_url, 'https://xcb.ntu.edu.cn/_upload/article/images/d6/d6/e334c5774d399d0bb686c31e6bb6/a401cc8e-8489-41f7-b7fb-06c10157f86b.png'),
  nearby_info = coalesce(nearby_info, 'The university has three campuses: Shengyuan campus at No. 9 Seyuan Road, Chongchuan District, Nantong (postcode 226019), Qixiu campus at No. 19 Qixiu Road (postcode 226001), and Qidong campus at No. 1 Nanhai Road, Qidong High-tech Zone (postcode 226236). Together they cover more than 3,300 mu.'),
  update_by = 'import-v107',
  update_time = now()
where id = @u;
