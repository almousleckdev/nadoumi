-- Gives the applications that already exist a reference, numbered per start day in the order they were created, and
-- seeds the day counters so new applications continue the sequence. Safe to run again.
--
-- manual rollback: update nad_application set reference = null where id > 0; delete from nad_application_reference_counter where ref_date is not null;

update nad_application a
  join (select id,
               date_format(date_add(create_time, interval 8 hour), '%Y%m%d') as day_key,
               row_number() over (partition by date(date_add(create_time, interval 8 hour)) order by id) as seq
          from nad_application) x on x.id = a.id
   set a.reference = concat('NAD', x.day_key, lpad(x.seq, 3, '0'))
 where a.id > 0 and a.reference is null;

insert into nad_application_reference_counter (ref_date, last_seq)
select date(date_add(create_time, interval 8 hour)), count(*)
  from nad_application
 group by date(date_add(create_time, interval 8 hour))
    on duplicate key update last_seq = greatest(last_seq, values(last_seq));
