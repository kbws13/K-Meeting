declare module 'lunar-calendar' {
  interface LunarDate {
    isLeap: boolean
    lunarMonthName: string
    lunarDayName: string
  }

  const LunarCalendar: {
    solarToLunar(year: number, month: number, day: number): LunarDate
  }

  export default LunarCalendar
}
