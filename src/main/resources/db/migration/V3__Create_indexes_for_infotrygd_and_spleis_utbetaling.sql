create view UTBETALINGER
AS
select UUID, FNR, MAX_DATE FORELOPIG_BEREGNET_SLUTT, UTBET_TOM UTBETALT_TOM, GJENSTAENDE_SYKEDAGER GJENSTAENDE_SYKEDAGER, OPPRETTET
from UTBETALING_INFOTRYGD
union all
select UUID, FNR, FORELOPIG_BEREGNET_SLUTT, TOM, GJENSTAENDE_SYKEDAGER, OPPRETTET
from UTBETALING_SPLEIS;

create index UTBETALING_SPLEIS_FNR_INDEX on UTBETALING_SPLEIS (FNR);
create index UTBETALING_SPLEIS_TOM_INDEX on UTBETALING_SPLEIS (TOM);
create index UTBETALING_INFOTRYGD_FNR_INDEX on UTBETALING_INFOTRYGD (FNR);
create index UTBETALING_INFOTRYGD_UTBET_TOM_INDEX on UTBETALING_INFOTRYGD (UTBET_TOM);