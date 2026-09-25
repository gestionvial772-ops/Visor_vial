export const SEVERIDAD= {
    LEVE: 1,
    MODERADO: 2,
    SEVERO: 3

} as const;


export const SEVERIDAD_LABELS: Record<number,string>={
    [SEVERIDAD.LEVE]: 'Leve',
    [SEVERIDAD.MODERADO]: 'Moderado',
    [SEVERIDAD.SEVERO]: 'Severo'
}